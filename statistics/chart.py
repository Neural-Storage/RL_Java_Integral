import os
import matplotlib.pyplot as plt
from matplotlib.pylab import figure
import numpy as np
import csv
import pandas as pd

data = [0, 1, 2]
threadIds = [9, 10, 11]
threadNum = [0, 1, 2]
threadColors = ['blue', 'red', 'orange']
fileNum = 3
precision = 3
dir = os.path.dirname(__file__)

# Plot Reward History
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(48, 15), dpi=80)
fig.suptitle(f'Responding Time for Each Thread')

for i, id, color in zip(threadNum, threadIds, threadColors):
    data[i] = pd.read_csv(os.path.join(dir, "avgMean", f"Thread-{id}.csv")) 
    data[i] = data[i].fillna(0)
    # print(data[i][' Sever InnerTime'].isnull().sum())
    # print(data[i].head)

    rtMean = round(np.mean(data[i]['AvgMeanTime(ms)']), precision)
    rtStd = round(np.std(data[i]['AvgMeanTime(ms)']), precision)
    rtPr99 = round(np.percentile(data[i]['AvgMeanTime(ms)'], 99), precision)
    rtMsg = f'Thread {id} Responding Time Moving Avg: {rtMean} | Std: {rtStd} | Slowest 99%: {rtPr99}(ms)'
    print(rtMsg)
    ax1.plot(data[i]['AvgMeanTime(ms)'], label = rtMsg, color = color)

    sitMean = round(np.mean(data[i][' Sever InnerTime']), precision)
    sitStd = round(np.std(data[i][' Sever InnerTime']), precision)
    sitPr99 = round(np.percentile(data[i][' Sever InnerTime'], 99, axis = 0), precision)
    sitMsg = f'Thread {id} Server Inner Time Moving Avg: {sitMean} | Std: {sitStd} | Slowest 99%: {sitPr99}(ms)'
    print(sitMsg)
    ax2.plot(data[i][' Sever InnerTime'], label = sitMsg, color = color)

ax1.set_xlabel('actions')
ax1.set_ylabel('Moving Average Time(ms) | ')
ax1.legend()
ax1.grid()

ax2.set_xlabel('actions')
ax2.set_ylabel('Sever InnerTime(ms) | ')
ax2.legend()
ax2.grid()

plt.savefig(os.path.join(dir, 'respond_time_chart.svg'))
plt.savefig(os.path.join(dir, 'respond_time_chart.png'))
plt.show()