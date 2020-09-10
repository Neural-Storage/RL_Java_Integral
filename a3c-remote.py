from multiprocessing import Process, Lock, Value, Array, Queue
import os
import ctypes
import tensorflow as tf
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.pylab import figure
# import tensorflow.compat.v1 as tfv1
import models.A2C as A2C
import envs.cartPole as cartPole
import envs.remote as remote
import envs.flappyBird as flappyBird
import models.util as util

LEARNING_RATE = 0.00001
DIR = os.path.dirname(__file__)

def drawChart(episode_results, worker_num):
    # Calculate average reward
    r_his = []
    loss_his = [] 
    worker_his = []
    avg_r_his = []
    r_last = 0
    for res in episode_results:
        r_his.append(res['reward'])
        loss_his.append(res['loss'])
        worker_his.append(res['worker_id'])

        r_last = 0.95 * r_last + res['reward'] * 0.05
        avg_r_his.append(r_last)

    # Plot Reward History
    # figure(num=None, figsize=(24, 6), dpi=80)
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(48, 6), dpi=80)
    fig.suptitle(f'Remote A3C Result with {worker_num} Workers & LR {LEARNING_RATE}')
    # x_datas = range(0, len(r_his))
    # avg_x_datas = range(0, EPISODE_NUM + 1, PRINT_EVERY_EPISODE)

    ax1.plot(r_his, color='blue')
    ax1.plot(avg_r_his, color='red')
    ax1.set_xlabel('Episodes')
    ax1.set_ylabel('Reward / Episode')
    ax1.grid()

    ax2.plot(loss_his, color='orange')
    ax2.set_xlabel('Episodes')
    ax2.set_ylabel('Loss / Episode')
    ax2.grid()
    
    plt.savefig(os.path.join(DIR, 'statistics', 'temp', f'Remote-A3C-res-w{worker_num}.svg'))
    plt.savefig(os.path.join(DIR, 'statistics', 'temp', f'Remote-A3C-res-w{worker_num}.png'))

class Master:
    def __init__(self):
        os.environ["CUDA_VISIBLE_DEVICES"] = "0"
        os.environ["OMP_NUM_THREADS"] = "1"
        os.environ["SDL_VIDEODRIVER"] = "dummy"
        tf.config.set_soft_device_placement(True)

    def worker(self, proc_id, worker_id, global_remain_episode, global_alive_workers, global_grad_queue, global_var_queue, global_res_queue):
        # server = tf.distribute.Server(cluster_spec, 'worker', worker_id)
        print(f'Process {proc_id} Worker {worker_id} start')

        # config = tf.compat.v1.ConfigProto()
        # config.gpu_options.allow_growth=True   
        # with tfv1.Session(target = server.target, config=config).as_default() as sess:
        # with tf.device("/job:localhost/replica:0/task:0/device:CPU:0"):

        gpus = tf.config.experimental.list_physical_devices('GPU')
        tf.config.experimental.set_memory_growth(gpus[0], True)
        # tf.config.experimental.set_virtual_device_configuration(gpus[0], [tf.config.experimental.VirtualDeviceConfiguration(memory_limit=10240)])
        with tf.device("/CPU:0"):
            local_agent, local_env = self.init_agent_env(proc_id, 'worker', worker_id)
            state = local_env.reset()
            while global_remain_episode.value > 0:
                episode_reward, loss, gradients, trajectory = local_agent.train_on_env(env = local_env, cal_gradient_vars = None)
                # print(f'Episode {global_remain_episode.value} Reward with worker {worker_id}: {episode_reward}')

                global_res_queue.put({'loss': loss, 'reward': episode_reward, 'worker_id': worker_id})
                global_grad_queue.put({'loss': loss, 'gradients': gradients, 'worker_id': worker_id})
                if not global_var_queue.empty():
                    global_vars = global_var_queue.get()
                    local_agent.model.set_weights(global_vars)
    #                 print(f'Worker {worker_id} Update Weights')

                with global_remain_episode.get_lock():
                    global_remain_episode.value -= 1

        with global_alive_workers.get_lock():
            global_alive_workers.value -= 1

        print(f"Worker {worker_id} done")

    def param_server(self, proc_id, ps_id, global_remain_episode, global_alive_workers, global_grad_queue, global_var_queues):
        # server = tf.distribute.Server(cluster_spec, 'ps', ps_id)

        # config = tf.compat.v1.ConfigProto()
        # config.gpu_options.allow_growth=True   
        # with tfv1.Session(target = server.target, config=config).as_default() as sess:
        # with tf.device("/job:localhost/replica:0/task:0/device:CPU:0"):

        gpus = tf.config.experimental.list_physical_devices('GPU')
        tf.config.experimental.set_memory_growth(gpus[0], True)
        # tf.config.experimental.set_virtual_device_configuration(gpus[0], [tf.config.experimental.VirtualDeviceConfiguration(memory_limit=10240)])
        with tf.device("/CPU:0"):
            global_agent, env = self.init_agent_env(proc_id, 'ps', ps_id)

            while ((not global_grad_queue.empty()) or (global_alive_workers.value > 0)):
    #             print(f'Getting gradients from queue')
                if not global_grad_queue.empty():
                    item = global_grad_queue.get()
                    global_agent.update(loss = item['loss'], gradients = item['gradients'])

                weights = global_agent.model.get_weights()
                for i in range(len(global_var_queues)):
                    if not global_var_queues[i].full():
                        global_var_queues[i].put(weights)
    #                     print(f'Put vars in queue for worker {i}')
            
            print("Complete PS apply")
            for queue in global_var_queues:
                if not queue.empty():
                    queue.get()
                    # print(f'Clear vars in queue for worker')
            print(f'PS {ps_id} done')

    def init_agent_env(self, proc_id, role, role_id):
        class Agent(A2C.Agent):
            def build_model(self, name):
#                 randUni = tf.keras.initializers.RandomUniform(minval=-0.05, maxval=0.05, seed=None)
                inputs = tf.keras.layers.Input(shape=self.state_size, name = 'inputs')
                # gru = tf.keras.layers.GRU(128, activation = 'tanh')(inputs)
                common = tf.keras.layers.Dense(128, activation="relu")(inputs)
                common = tf.keras.layers.Dense(128, activation="relu")(common)
                action = tf.keras.layers.Dense(self.num_action, activation="softmax", name = 'action_outputs')(common)
                critic = tf.keras.layers.Dense(1, name = 'value_output')(common)

                model = tf.keras.Model(inputs=inputs, outputs=[action, critic])

                return model

        # env = remote.RemoteEnv()
        env = cartPole.CartPoleEnv()
        # env = flappyBird.FlappyBirdEnv()
        NUM_STATE_FEATURES = env.get_num_state_features()
        NUM_ACTIONS = env.get_num_actions()
        PRINT_EVERY_EPISODE = 20
#         LEARNING_RATE = 0.03
        REWARD_DISCOUNT = 0.99
        COEF_VALUE= 1
        COEF_ENTROPY = 0
#         agent = A2C.Agent((NUM_STATE_FEATURES, ), NUM_ACTIONS, REWARD_DISCOUNT, LEARNING_RATE, COEF_VALUE, COEF_ENTROPY)
        agent = Agent((NUM_STATE_FEATURES, ), NUM_ACTIONS, REWARD_DISCOUNT, LEARNING_RATE, COEF_VALUE, COEF_ENTROPY)

        return agent, env

    def make_cluster_config(self, ps_num, worker_num, host = 'localhost', base_port = 8000):
        cluster_config = {
            'worker': [],
            'ps': []
        }
        port = base_port

        for ps_id in range(self.ps_num):
            cluster_config['ps'].append(f'{host}:{port}')
            port += 1

        for worker_id in range(self.worker_num):
            cluster_config['worker'].append(f'{host}:{port}')
            port += 1
        
        return cluster_config

    # def is_having_training_info(self):
    #     return ((not global_res_queue.empty()) or (global_alive_workers.value > 0))
    # def get_res(self, global_res_queue, global_alive_workers):
    #     if ((not global_res_queue.empty()) or (global_alive_workers.value > 0)):
    #         return global_res_queue.get()
    #     else:
    #         return None

    def start(self, episode_num, ps_num, worker_num):
        # print(tf.config.experimental.list_physical_devices(device_type=None))
        # print(tf.config.experimental.list_logical_devices(device_type=None))

        self.episode_num = episode_num
        self.ps_num = ps_num
        self.worker_num = worker_num
        self.current_episode = 1
        global_remain_episode = Value('i', self.episode_num)
        global_alive_workers = Value('i', self.worker_num)
        global_res_queue = Queue()
        global_grad_queue = Queue()
        global_var_queues = [Queue(1) for i in range(self.worker_num)]
        cluster_config = self.make_cluster_config(self.ps_num, self.worker_num)

        pss = []
        workers = []
        episode_results = []

        cluster_spec = tf.train.ClusterSpec(cluster_config)
        
        for ps_id in range(self.ps_num):
            pss.append(Process(target = self.param_server, args=(ps_id, ps_id, global_remain_episode, global_alive_workers, global_grad_queue, global_var_queues)))

        for worker_id in range(self.worker_num):
            workers.append(Process(target = self.worker, args=(worker_id + self.ps_num, worker_id, global_remain_episode, global_alive_workers, global_grad_queue, global_var_queues[worker_id], global_res_queue)))

        for num in range(self.ps_num):
            pss[num].start()

        for num in range(self.worker_num):
            workers[num].start()

        r_his = []
        loss_his = [] 
        worker_his = []
        avg_r_his = [0]

        writer = util.Writer(os.path.join(DIR, 'statistics', 'temp', 'CartPole-A3C-res-w.csv'))
        writer.set_header(['reward', 'avg_reward', 'loss'])

        while ((not global_res_queue.empty()) or (global_alive_workers.value > 0)):
            if not global_res_queue.empty():
                episode_res = global_res_queue.get()
                episode_results.append(episode_res)
                
                episode_avg_reward = 0.05 * episode_res['reward'] + 0.95 * avg_r_his[-1]
                r_his.append(episode_res['reward'])
                loss_his.append(episode_res['loss'])
                worker_his.append(episode_res['worker_id'])
                avg_r_his.append(episode_avg_reward)
                
                writer.write_row([episode_res['reward'], round(episode_avg_reward, 3), episode_res['loss'].numpy()])

                print(f"Episode {self.current_episode} Reward with worker {episode_res['worker_id']}: {episode_res['reward']}\t| Loss: {episode_res['loss']}")
                self.current_episode += 1
        
        drawChart(episode_results, worker_num)

        global_grad_queue.close()
        global_grad_queue.join_thread()

        global_res_queue.close()
        global_res_queue.join_thread()

        for queue in global_var_queues:
            queue.close()
            queue.join_thread()

        for num in range(self.worker_num):
            workers[num].join()
            print(f'Worker {num} join')

        for num in range(self.ps_num):
            pss[num].join()
            print(f'PS {num} join')

if __name__ == '__main__':
    # print(tf.config.experimental.list_physical_devices(device_type=None))
    # print(tf.config.experimental.list_logical_devices(device_type=None))

    m = Master()
    m.start(100, 1, 2)

    
    # writer.write_row([1, 2, 3])
    # writer.write_batch([[3, 2, 1], [4, 5, 6]])
    # writer.close()