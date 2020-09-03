import matplotlib.pyplot as plt
from matplotlib.pylab import figure
import numpy as np
import csv
import pandas as pd

# avgMeans9 = []
# serverInnerTimes9 = []
# avgMeans10 = []
# serverInnerTimes10 = []
# avgMeans11 = []
# serverInnerTimes11 = []

dataT9 = pd.read_csv("avgMean/Thread-9.csv") 
print(dataT9.head)
dataT10 = pd.read_csv("avgMean/Thread-10.csv") 
print(dataT10.head)
dataT11 = pd.read_csv("avgMean/Thread-11.csv") 
print(dataT11.head)
# print(type(dataT9))
# print(data['AvgMeanTime(ms)'])
# with open(f'avgMean/Thread-9.csv', 'r') as file:
#     reader = csv.reader(file)
#     i = 0
#     for row in reader:
#         if i > 0:
#             print(row[0])
#             print(row[1])
#             avgMeans9.append(float(row[0]))
#             serverInnerTimes9.append(float(row[1]))
#         i += 1

# with open(f'avgMean/Thread-10.csv', 'r') as file:
#     reader = csv.reader(file)
#     i = 0
#     for row in reader:
#         if i > 0:
#             avgMeans10.append(float(row[0]))
#             serverInnerTimes10.append(float(row[1]))
#         i += 1

# with open(f'avgMean/Thread-11.csv', 'r') as file:
#     reader = csv.reader(file)
#     i = 0
#     for row in reader:
#         if i > 0:
#             avgMeans11.append(float(row[0]))
#             serverInnerTimes11.append(float(row[1]))
#         i += 1

# print(avgMeans9)
# print(serverInnerTimes9)

# Plot Reward History
# figure(num=None, figsize=(24, 6), dpi=80)
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(48, 15), dpi=80)
fig.suptitle(f'Responding Time for Each Thread')
# x_datas = range(0, len(r_his))
# avg_x_datas = range(0, EPISODE_NUM + 1, PRINT_EVERY_EPISODE)

ax1.plot(dataT9['AvgMeanTime(ms)'], color='blue')
ax1.plot(dataT10['AvgMeanTime(ms)'], color='red')
ax1.plot(dataT11['AvgMeanTime(ms)'], color='orange')
ax1.set_xlabel('actions')
ax1.set_ylabel('Moving Average Time(ms) | ')
ax1.grid()

ax2.plot(dataT9[' Sever InnerTime'], color='blue')
ax2.plot(dataT10[' Sever InnerTime'], color='red')
ax2.plot(dataT11[' Sever InnerTime'], color='orange')
ax2.set_xlabel('actions')
ax2.set_ylabel('Sever InnerTime(ms) | ')
ax2.grid()

plt.savefig('respond_time_chart.svg')
plt.show()