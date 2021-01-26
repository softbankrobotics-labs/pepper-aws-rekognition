# Pepper Recognition

#### Note: This library may use the Pepper camera to take photos of humans, and store them  remotely. When using this library, please ensure your application complies to the European General Data Protection Regulation (GDPR) rules. More information is available [here](https://gdpr-info.eu/).


This Android library will help you:

* Recognize humans by learning and later recognizing their faces

This library uses the Amazon AWS Rekognition API to do the face learning and recognition, and Amazon AWS Cognito to secure the calls to Rekognition.

## 1. Video demonstration

*To be added soon*

## 2. Amazon AWS services

This library uses the Amazon AWS Rekognition API to do the face learning and recognition, and Amazon AWS Cognito to secure the calls to Rekognition.

### 2.1 Amazon AWS Rekognition

We reproduce here the general presentation found in its documentation. If you want a more in depth description of what Rekognition does, please refer to [this doc](https://docs.aws.amazon.com/rekognition/latest/dg/what-is.html).

*Amazon Rekognition makes it easy to add image and video analysis to applications. You just provide an image or video to the Amazon Rekognition API, and the service can identify objects, people, text, scenes, and activities. It can detect any inappropriate content as well. Amazon Rekognition also provides highly accurate facial analysis, face comparison, and face search capabilities. You can detect, analyze, and compare faces for a wide variety of use cases, including user verification, cataloging, people counting, and public safety.*

### 2.2 Amazon AWS Cognito

We reproduce here the general presentation found in its documentation. If you want a more in depth description of what Rekognition does, please refer to [this doc](https://docs.aws.amazon.com/cognito/latest/developerguide/what-is-amazon-cognito.html).

*Amazon Cognito provides authentication, authorization, and user management for your web and mobile apps. Your users can sign in directly with a user name and password, or through a third party such as Facebook, Amazon, Google or Apple.
The two main components of Amazon Cognito are user pools and identity pools. User pools are user directories that provide sign-up and sign-in options for your app users. Identity pools enable you to grant your users access to other AWS services.*

### 2.3 Pricing of those services

AWS provides a free tier, so if you just signed up to Amazon AWS, you'll be able to recognize 5000 users images free of charge for the first 12 months:

| Amazon Service  | Pricing |
|--|--|
| Amazon Cognito | Free for up to 50000 Monthly active users per month |
| Amazon Rekognition | Free for 5000 images per month the first 12 months of subscription to AWS. Then 0.001$ per image (can be more in certain AWS regions) |

## 3. Getting started

### 3.1 Create an Amazon AWS Account

This library relies on Amazon AWS Rekognition. As such it requires that you have an AWS account, and you provide valid AWS credentials. If you don't have an Amazon AWS account, create one using the instructions provided here: [https://aws.amazon.com/premiumsupport/knowledge-center/create-and-activate-aws-account/](https://aws.amazon.com/premiumsupport/knowledge-center/create-and-activate-aws-account/)

### 3.2 Choose an Region where Rekognition is available

Amazon services are hosted in multiple locations world-wide. These locations are represented by Regions, which are separate geographic area.
Amazon Rekognition is not available in all regions. Here is the list of Regions where it is available:

* Asia Pacific (Mumbai) `ap-south-1`
* Europe (London) `eu-west-2`
* Europe (Ireland) `eu-west-1`
* Asia Pacific (Seoul) `ap-northeast-2`
* Asia Pacific (Tokyo) `ap-northeast-1`
* Asia Pacific (Singapore) `ap-southeast-1`
* Asia Pacific (Sydney) `ap-southeast-2`
* Europe (Frankfurt) `eu-central-1`
* US East (N. Virginia) `us-east-1`
* US East (Ohio) `us-east-2`
* US West (N. California) `us-west-1`
* US West (Oregon) `eu-west-2`

You need to decide in which region you want to use Amazon Rekognition. If you deploy an app in Europe, you might choose the Region *Europe (Ireland)* (or *Europe (London)*). Then Rekognition will store all user face data on servers located in the Europe region, and more specifically in Ireland (or England).

For the illustrative purpose of this README, we will choose *Europe (Ireland)*, codename `eu-west-1`

***IMPORTANT: Make a note of the Region, you will need it later to run the Sample App***

Whatever region you choose, make sure you comply with your local regulation regarding user data protection.

### 3.3 Install the Amazon AWS CLI for easier configuration of the AWS account

There are several possibility to configure an Amazon AWS account. You can use the [aws console website](https://console.aws.amazon.com/console/home) or you can use the [command line client AWS CLI](https://aws.amazon.com/cli/).

In the reminder of this README we chose to provide some of the configuration steps as command lines using the AWS CLI. So install the AWS CLI version 2 and set it up with an IAM user credentials using the following guide:

[https://docs.aws.amazon.com/rekognition/latest/dg/setup-awscli-sdk.html](https://docs.aws.amazon.com/rekognition/latest/dg/setup-awscli-sdk.html)


### 3.4 Create an Amazon Rekognition Collection resource that will store all faces data

Amazon Rekognition stores information about detected faces in server-side containers known as collections.
So the face collection is the primary Amazon Rekognition resource, and each face collection you create has a unique Amazon Resource Name (ARN). You create each face collection in a specific AWS Region in your account.

Use the AWS CLI to create a collection named "PepperRecognition"

```
$ aws rekognition create-collection \
    --region eu-west-1 \
    --collection-id "PepperRecognition"
```

***IMPORTANT: Make a note of the CollectionARN, you will need it later to run the Sample App***

Replace `eu-west-1` by the region you have chosen if you have chosen a different one.

### 3.5. Create an Amazon Cognito identity pool, to configure access to the Amazon Recognition service

Now we need to configure who can access our collection in Amazon Recognition, send faces and search for known faces. We don't want the service to be opened to the public. Ideally, we only want authenticated users to be able to access our recognition collection. To do that, we will the Amazon Cognito service, and in that service, we will use Identity Pool and Identity providers.
This is the place where we say what authenticated users can do (access our collection), and what unauthenticated user can do (nothing).

*There are 2 ways to proceed. You can use the CLI as we have done in the previous step, or you can set this up using the AWS GUI. 
To use the CLI instructions, follow this link: [CLI Setup](doc/aws_cli_setup.md)
For GUI instructions click here: [GUI Setup](doc/aws_gui_setup.md)*

### 3.6. Generate a temporary access token

Finally, we generate a temporary access token, valid 24 hours, that will allow us to run the sample app and make queries to Rekognition as our authenticated user "MyUser1".

```
$ aws cognito-idp admin-initiate-auth \
    --region eu-west-1 \
    --user-pool-id eu-west-1_AA1122aab \
    --client-id 2hhhiiijjj111appclientidxx \
    --auth-flow ADMIN_NO_SRP_AUTH \
    --auth-parameters 'USERNAME=MyUser1,PASSWORD=A/skzw12'
```

*Output:*
```
{
    "ChallengeParameters": {},
    "AuthenticationResult": {
        "AccessToken": "eyxXxXxXx.eyxXxXxXxXxXxXx.xXxXxXxXx",
        "ExpiresIn": 3600,
        "TokenType": "Bearer",
        "RefreshToken": "eyxXxXxXx.xXxXxXxXx.xX.xXxXxXxXxX",
        "IdToken": "eyxXxXxXx.eyxXxXxXxXxXxXx.xXxXxXxXx"
    }
}
```
***IMPORTANT: Make a note of the IdToken, you will need it later to run the Sample App***

### 3.7. Running the sample app

The project comes with a sample application. You can clone the repository and open it in Android Studio.
You will need to provide AWS credentials so that the sample application can connect to your amazon account and run face recognition requests.

Create a file named `local.properties` in the root folder of this repository, and write:

- the Rekognition Collection ID (Obtained in section 3.4)
- the AWS region (From section 3.2)
- the identity pool id (From section 3.5)
- the user pool provider name, in the form: cognito-idp.< REGION >.amazonaws.com/< USER POOL ID from section 3.5)>
- the user id token from section 3.6

In our case, it gives:

```
AWS_COLLECTION_ID=PepperRecognition
AWS_REGION=eu-west-1
AWS_IDENTITY_POOL=eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb
AWS_USER_POOL_PROVIDER_NAME=cognito-idp.eu-west-1.amazonaws.com/eu-west-1_AA1122aab
AWS_USER_ID_TOKEN=eyXXXX.eyXXXX.XXX
```

Once you have created this file, you can run the sample application on Pepper.

### 3.8. Sample app usage and description

In the sample app, Pepper waits to detect humans. Once it has engaged a human, Pepper detects their face and displays it on the tablet. It also tries to decide which human it is, by recognizing their face.
If the human is unknown, Pepper will assign a number to the user, based on how many users it already has recognised (the first person will be 1, second person 2 and so on...).
Subsequently, when Pepper engages the same human, it will recognize him and greet him by his number.

You can modify this behaviour based on your use case, for example, saving names or attributes to give Pepper the impression of having a memory.


## 4. Using the library in your project


### 4.1. Add the library as a dependancy

[Follow these instructions](https://jitpack.io/#softbankrobotics-labs/pepper-recognition)

Make sure to replace 'Tag' by the number of the version of the library you want to use, or by 'master-SNAPSHOT' to use the master branch.


## 5. Usage


### 5.1. Human face recognition

#### 5.1.1 Description

Every time Pepper detects or engages a [`Human`](https://qisdk.softbankrobotics.com/sdk/doc/pepper-sdk/ch4_api/perception/reference/human.html#human), this library learns the human face and assigns him a unique id. Later on, whenever the same `Human` is recognized the lib will return the same unique id.

To obtain the `Human` unique id, create a `HumanFaceRecognition` object, and then call the `addOnFaceIdAvailableListener` function. You will have to give to the `HumanFaceRecognition` constructor the following parameters:

- The AWS Identity Pool and Region created in section 3.5, step 4
- The Aws region (from section 3.2)
- A login map, composed of the user pool provider name, in the form: cognito-idp.< REGION >.amazonaws.com/< USER POOL ID from section 3.6 step 1)>, and the user id token from section 3.7
- The Rekognition collection id (obtained in section3.4)

```kotlin
HumanFaceRecognition(
    applicationContext,
    BuildConfig.AWS_IDENTITY_POOL,
    hashMapOf(BuildConfig.AWS_USER_POOL_PROVIDER_NAME to BuildConfig.AWS_USER_ID_TOKEN),
    BuildConfig.AWS_REGION,
    BuildConfig.AWS_COLLECTION_ID //Default is MyCollection
)

val callback = object: OnFaceIdAvailableListener {
    override fun onFaceIdAvailable(faceId: String) {
        Log.i("Test", "The human unique id is ${faceId}")
    }
}

humanFaceReco.addOnFaceIdAvailableListener(human, callback)
```

The method `onFaceIdAvailable` of the `callback` object will be called whenever the lib has detected and recognized the human face. The unique id `faceId` will be given as parameter to the `onFaceIdAvailable` method.

The `HumanFaceRecognition` class has the following interface:

```kotlin
  constructor(context: Context, identityPoolId: String, logins: Map<String, String>, region: String, collectionId: String): this() {
      this.collectionId = collectionId
      this.faceRecognition = AWSFaceRecognition(context, identityPoolId, region)
  }

    // Retrieve the face id of a Human
    fun getFaceId(human: Human): String?

    // Add a listener object that will be called whenever the Human is recognized
    fun addOnFaceIdAvailableListener(human: Human, listener: OnFaceIdAvailableListener)

    // Remove a listener associated to a Human
    fun removeOnFaceIdAvailableListener(human: Human, listener: OnFaceIdAvailableListener): Future<Void>

    // Remove all listeners associated to a Human
    fun removeAllOnFaceIdAvailableListener(human: Human): Future<Void>
}
```

#### 5.1.2 Example sequence diagram

Here is a sequence diagram that illustrates what happens when you subscribe to HumanFaceRecognition with addOnFaceIdAvailableListener

<img src="img/PepperRecognition.jpg" alt="" width="900" border="10"/>

### 5.2. Manage the calls to AWS

There is a lower level class available, that allows you to manage the calls to the AWS apis directly.

```kotlin
class AWSFaceRecognition(context: Context, identityPoolId: String, logins: Map<String, String>, region: String, asyncRequestPoolSize: Int = 1) {


    fun recognizeFaceAsync(collectionId: String, face: TimestampedImage): Future<String?>

    fun recognizeFaceAsync(collectionId: String, face: Bitmap): Future<String?>

    fun recognizeFace(collectionId: String, face: TimestampedImage): String?

    fun recognizeFace(collectionId: String, face: Bitmap): String?

    fun deleteCollection(collectionId: String)

    fun deleteFaces(collectionId: String, facesId: List<String>)
}
```


The AWSFaceRecognition constructor takes 4 parameters:

 - The AWS Region and Identity Pool and Logins map
 - The size of the threadpool used to run the asynchronous requests. By default only one thread is used, so only one request can run at a time, and all other face recognition request will be rejected.

All functions in the AWSFaceRecognition class try to find faces in the 'face' image, and to recognize the first one it finds. If the face is unknown, it will be learnt.

They take as argument:

- 'face': the face image
- 'collectionId': A unique string (Collection ID) that AWS will use to name of the collection
  where it will store all the faces characteristics of the humans the library will learn and
  later recognize.

Async versions of the function returns a Future containing the unique id of the face recognized.
The id string will be 'null' if no face is found. The future will contains an error if the
threadpool used to run the request is already full with running requests.

Synchronous versions of the function will directly return the id string, and will block the current thread.

### Appendix

#### AWS Rekognition CLI Commands

If ever you need to delete this collection, you can do it easily with the following AWS cli command:
```
$ aws rekognition delete-collection \
    --region < REGION > \
    --collection-id "PepperRecognition"
```

If you want to list all collections, use:
```
$ aws rekognition list-collections \
    --region < REGION >
```

## 6. User privacy

Please comply with local regulation. For instance in Europe you must not store user photos without their consent. In order to remove all users photo from AWS cloud, you can use the functions `deleteCollection` and `deleteFaces` of the `AWSFaceRecognition` class.

## 7. License

This project is licensed under the BSD 3-Clause "New" or "Revised" License- see the [COPYING](COPYING.md) file for details
