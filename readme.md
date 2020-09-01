# RL & Java Environment Integral

Requirement:

- Tensorflow 2.0 with GPU
- Numpy
- CUDA
- Python 3.7

You can setup the environment with Anaconda. 

If you don't have Java, follow the following instructions:

> cd java
> 
> wget https://javadl.oracle.com/webapps/download/AutoDL?BundleId=242980_a4634525489241b9a9e1aa73d9e118e6
> 
> tar zxvf AutoDL?BundleId=242980_a4634525489241b9a9e1aa73d9e118e6
> 

To start the DEMO, type following command:

Server:
> ./java/jre1.8.0_261/bin/java -jar java/Multi_Thread_SeverTest/Jars/FlappyBird.jar

If you've already have Java in your computer, type:

> java -jar java/Multi_Thread_SeverTest/Jars/FlappyBird.jar

Client:
> python a3c-remote.py
