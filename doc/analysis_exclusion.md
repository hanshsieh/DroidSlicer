# Analysis Exclusion #
In configuration file *config/apiXXX/regression_exclusions.txt* (XXX is the Android API level), the packages and classes that should be ignored during analysis can be defined. In the file, each line will be treated as a regular expression. Any class whose fully qualified class name matches a regular expression in the file will be treated as absent during analysis.

For example, if you use the Jar file for Java library on PC as one of Android library files in the analysis, classes under package *java.awt* will be bring into the analysis. However, classes under *java.awt* actually do not exist in Android library. To exclude the classes under this package, you can add the following line to the configuration file.

    java\/awt\/.*

Notice that when matching a regular expression with the fully qualified class names, '/' will be used as the package seperator, instead of '.'.
