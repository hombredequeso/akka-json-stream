Example using Akka Streams to process JSON data.

Test Setup
Upload test files to S3

```
aws s3 ls
# find or create the bucket to use
export bucketname=NAME_OF_S3_BUCKET
cd src/test/resources 
aws s3 cp . s3://${bucketname}/akka-quickstart-scala/ --recursive
```

