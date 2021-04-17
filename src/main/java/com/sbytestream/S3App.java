package com.sbytestream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
public class S3App implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(S3App.class, args);
    }

    @Override
    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            help();
            return;
        }

        cmd = new CmdLine(args);
        String awsRegion = cmd.getFlagValue(FLAG_AWS_REGION);
        if (awsRegion == null || awsRegion == "") {
            System.out.println("Region has not been specified!");
            return;
        }
        Regions regions = Regions.fromName(awsRegion);

        String accessKey = cmd.getFlagValue(FLAG_ACCESS_KEY_ID);
        if (accessKey == null || accessKey == "") {
            System.out.println("Access key has not been specified!");
            return;
        }

        String secretKey = cmd.getFlagValue(FLAG_SECRET_KEY_ID);
        if (secretKey == null || secretKey == "") {
            System.out.println("Secret key has not been specified!");
            return;
        }

        String bucketName = cmd.getFlagValue(FLAG_BUCKET_NAME);
        if (bucketName == null || bucketName == "") {
            System.out.println("Bucket name is empty");
            return;
        }

        String objectKey = cmd.getFlagValue(FLAG_OBJECT_KEY);
        if (objectKey == null || objectKey == "") {
            System.out.println("Object key has not been specified");
            return;
        }

        String expr = cmd.getFlagValue(FLAG_EXPR);
        if (expr == null || expr == "") {
            System.out.println("Expression has not been specified");
            return;
        }
        logger.info(String.format("SQL: %s", expr));

        try {
            runQuery(regions, accessKey, secretKey, bucketName, objectKey, expr);
        }
        catch(Exception e) {
            logger.error("An exception occurred while running query", e);
            System.out.println(String.format("Something bad happened: %s. See log file in logs\\ folder for details.",
                    e.getMessage()));
        }
    }

    private void help() {
        System.out.println("Runs an select query on csv data stored in an S3 bucket");
        System.out.println("Syntax: java -jar s3sel-1.0.jar  -rgn <aws-region-name> -akey <aws-access-key> -skey <aws-secret-key> -bucket <s3-bucket-name> -okey <s3-object-key> -expr \"<select-query>\"");
        System.out.println("Example:");
        System.out.println("java -jar s3sel-1.0.jar -rgn us-east-1 -akey MYACCESSKEY -skey MYSECRETKEY -bucket my-s3-sucket -okey books.csv -expr \"select title,price from s3object s where s.author = 'Isaac Asimov'\"");
        System.out.println("Web: https://sbytestream.pythonanywhere.com");
    }

    private void runQuery(Regions regions, String accessKey, String secretKey, String bucket, String objectKey, String sql) throws Exception {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(regions)
                .build();

        SelectObjectContentRequest socr = new SelectObjectContentRequest();
        socr.setBucketName(bucket);
        socr.setKey(objectKey);
        socr.setExpressionType("SQL");
        socr.setExpression(sql);

        CSVInput csvInput = new CSVInput();
        csvInput.setFileHeaderInfo("Use");
        csvInput.setFieldDelimiter(",");

        InputSerialization iser = new InputSerialization();
        iser.setCsv(csvInput);
        iser.setCompressionType("NONE");
        socr.setInputSerialization(iser);

        CSVOutput csvOutput = new CSVOutput();
        csvOutput.setFieldDelimiter(",");

        OutputSerialization oser = new OutputSerialization();
        oser.setCsv(csvOutput);
        socr.setOutputSerialization(oser);

        Instant startedAt = Instant.now();
        SelectObjectContentResult result = s3client.selectObjectContent(socr);
        InputStream resultInputStream = result.getPayload().getRecordsInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(resultInputStream, "UTF-8"));
        String line;
        int matchedRecords = 0;
        while ((line = streamReader.readLine()) != null) {
            System.out.println(line);
            matchedRecords++;
        }
        Instant endedAt = Instant.now();
        System.out.printf("Got %d records in %d ms\n", matchedRecords, Duration.between(startedAt, endedAt).toMillis());
    }

    private static CmdLine cmd;
    private static final Logger logger = LoggerFactory.getLogger(S3App.class);
    private final String FLAG_AWS_REGION = "rgn";
    private final String FLAG_ACCESS_KEY_ID = "akey";
    private final String FLAG_SECRET_KEY_ID = "skey";
    private final String FLAG_BUCKET_NAME = "bucket";
    private final String FLAG_OBJECT_KEY = "okey";
    private final String FLAG_EXPR = "expr";
}
