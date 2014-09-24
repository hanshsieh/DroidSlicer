# Abstract #
DroidSlicer is a static analysis tool to discover sensitive behaviors in an Android app. We define a sensitive behavior to be access and usage of sensitive data. The sensitive data we take into consideration include permission protected data and data from the file system.

For the detail introduction of the system, please see the paper. Here, we only cover the overview of the system, and how to use this tool.

# Getting Started #
There's another more detailed user guide at the **doc** directory.  
The user guide is written in markdown format.  You may read it directly, or follow the steps below to generate the html.  

First, you should have [gradle], a tool for build automation.  
You can download the binaries from the official site of [gradle], or you can use our wrapper scripts, which will download the binaries for you for the first time.  
On Windows, use **gradlew.bat**. On Linux, use **gradlew**.  

In the following, we assume that **gradle** is in your **PATH**, and if you are using the wrapper scripts, just replace the **gradle** command to the path of the wrapper scripts.

To generate the HTML of the user guide, use the following command.  

    gradle exportDoc

The generated HTML files will be put into directory **build/export/doc**. 

[gradle]:http://www.gradle.org
