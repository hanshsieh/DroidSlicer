# System Overview #
This tool is written in pure Java, and it should be able to run on any platform with JRE.
This system have several entry points. You may choose the entry point that fit your need.

# Compile #
This tool is provided as an [eclipse] project.  

The dependencies are maintained using [gradle], and the configuration of [gradle] is in file build.gradle.  
You may compile the code using [gradle], or [eclipse].  
If you just want to use the tool, instead of development, using [gradle] is recommanded, since we have written some simple scripts for exporting and archiving.  

To compile the code using [eclpse], you need to first generate the necessary files for eclipse using the following command.

	gradle eclipse

Then, import the project into [eclipse], and [eclipse] will automatically compile the code for you.  
If you have changed the dependencies in **build.gradle**, you need to run the command above again, and refresh the project in eclipse.  

In the following, we will illustrate how to compile and export the project using [gradle].  

We assume that **gradle** is in your **PATH**.  
If not, you may also use the wrapper script.  
On Windows, use **gradlew.bat**. On Linux, use **gradlew**.  

To export all the necessary files for running this tool as well as the document, use the following command.

    gradle export

The generated files will be put into directory **build/export**. 

To just compile and archive the code, use the following command.  

    gradle jar

The compiled class files will be put into directory **build/classes**.  
The jar file will be put into directory **build/libs**.  

To just generate the HTML of this document, use the following command.  

    gradle exportDoc

The generated HTML document will be put into directory **build/export/doc**.   

To just generate the javadoc, use the following command.

    gradle exportJavadoc

The generated javadoc will be put into **build/export/doc/apidoc**.

To clean the generated files, use the following command.

    gradle clean

# Entry Points #
## Main Entry Point (with GUI) ##
It is the main entry point of the tool, and GUI is provided.  

- class: org.droidslicer.DroidSlicer  
- arguments: [-app <apk>]  

The optional argument "-app" allow you to start the GUI and directly analyze an app immediately.  
If no argument is specified, you may start the analysis by the manipulation of the GUI.

Currently, the GUI is very simple.  
![gui interface]  
The top panel is used to show the sensitive behavior graph.  
The bottom panel is used to show the properties of the selected node.
There's another panel hidden by default, and is used to show the log message. Click the "Log" tab at the bottom to show it.  
You can drag-and-drop the panels to change the layout.

To start analyzing an app, use "File" > "Open an APK...".  
After the analysis finishes, sensitive behaivor graph will be shown, and you can click on a node to see the properties.

## Project Structure ##
### libs/ ###
Dependent library files that we failed to find the appropriate online repositories.  

### libs/android4me.AXMLPrinter2/ ###  
The library used to parse the binary **Manifest.xml**.  

### libs/att.grappa/ ### 
The library used to parse a DOT file.  

### libs/com.mxgraph.jgraphx/ ### 
The library used to visualize the sensitive behavior graph.  

### libs/heros/ ### 
The library used to do IFDS. We currently decided not to use this library, and use the implementation of IFDS in [Wala] instead.  

### libs/org.jgrapht/ ### 
The library used to maintain the graph structure.  

### libs/wala/ ### 
The library used for call graph construction, pointer analysis, IFDS, etc.
src: Source code.  

### doc/images/ ### 
The image files for this document.  

### config/ ### 
This directory contains the configuration files, Android library files, auxiliary data, etc. For the details, see [this](config.html) page. 

# Javadoc #
See [here](apidoc/index.html)

[Wala]:http://wala.sourceforge.net
[gui interface]:images/gui.jpg
[PScout]:http://pscout.csl.toronto.edu
[gradle]:http://www.gradle.org
[eclipse]:https://www.eclipse.org
