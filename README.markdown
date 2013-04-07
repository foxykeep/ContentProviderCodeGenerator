DatabaseCodeGenerator
=====================

This project is a code generator written in Java used to generate Android code. Given a database schema JSON definition file, it will generate all the code you need to add the corresponding ContentProvider in your project.


Architecture of the project
---------------------------

The java project found in the folder 'generator' contains the following folders :
 * src : The code used to generate your ContentProvider. Normally you shouldn't have to modify it.
 * res : This folder contains snippets of code used by the generator.
 * input : This folder contains your database schema JSON definition files. You can have multiple JSON files next to each other. An example and the JSON format are available in the example subfolder.
 * output : This is where the generated code will be written. Each generate code will be stored in a folder based on the corresponding JSON file name to separate them. 


How to use ContentProviderCodeGenerator
--------------------------------

In order to use it, here are the steps to follow :

1. Download the current version of the project on your computer using git (`git clone git@github.com:foxykeep/ContentProviderCodeGenerator.git`). 
2. Import the project in the folder 'generator' in your Eclipse workspace. 
3. Write your JSON definition file. You can use the format file and the example file given in the folder input/example to help you.
4. Run the project as a Java application.
5. Your classes are available in the output folder

Building/Running from the CLI
-----------------------------
```
    mkdir generator/bin
    mkdir generator/bin/input
    cd generator/bin
    ln -s ../res .
    javac $(find ../src -name *.java) -d .
    mkdir input
    cp ../example/sample.json ./input
    java com/foxykeep/cpcodegenerator/Main
```

Example
-------

You can find in the 'sample' folder an example of an Android application which uses the generated ContentProvider


TODO
----

- Add a sample project which uses a generated ContentProvider


Credits and License
-------------------

Foxykeep ([http://foxykeep.com](http://foxykeep.com/))

Licensed under the Beerware License :

> You can do whatever you want with this stuff. If we meet some day, and you think this stuff is worth it, you can buy me a beer in return.
