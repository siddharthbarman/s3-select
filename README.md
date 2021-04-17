# s3-select
Demonstrates using AWS S3 client to query csv file stored in an S3 bucket.
This sample was written to accompany blog post of the same topic located at:
https://sbytestream.pythonanywhere.com/blog/S3-Select

# Building and running the code
You need to have JDK 1.8 and Maven installed. 
Use maven to compile the code:
mvn compile

Use maven to package and build the jar:
mvn package

The output jar is stored in the target folder named s3sel-1.0.jar

# Running the sample
The data\ folder contains the sample books.csv file. You need to upload the 
file to an S3 bucket. You will need to have AWS API access-key and secret-key
to connect to S3 service and run the program.

Running the program with no arguments will display the help:
java -jar s3sel-1.0.jar

Running the program will required arguments:
java -jar s3sel-1.0.jar -rgn us-east-1 -akey MYACCESSKEY -skey MYSECRETKEY -bucket my-s3-sucket -okey books.csv -expr \"select title,price from s3object s where s.author = 'Isaac Asimov'\""

Logs are stored in Logs\ folder.