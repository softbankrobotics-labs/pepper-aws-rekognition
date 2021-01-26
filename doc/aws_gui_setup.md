# AWS GUI Instructions

### Create an Amazon Cognito identity pool, to configure access to the Amazon Recognition service

These instructions will configure the Cognito service using the AWS GUI. Note that there are still some CLI calls which are needed, but the majority of what is done here is doing in the AWS Console.

There are many ways to authenticate users with Amazon. You can use custom authentication, or define login password in Amazon Cognito, or use federated identity like Google or Facebook.
We chose to set login and passwords directly in Amazon Cognito. And to simplify things a little bit, we will directly get authentication tokens using AWS cli, and copy paste them in our sample app, so that you won't have to login in the Android sample app on Pepper.

Follow these steps:

1) We need to provide a User to authenticate the calls. We'll do this in Cognito. Open Cognito from the services tab, and press Manage User Pools. Create a new one. Name it **PepperRecognitionUsers**. 
![Create User Pool](doc/CreateUserPool.png)

2) Press Review Defaults, and in the App Clients section, create a new one. Name it **PepperRekognitionLoginApp** and tick the box for ALLOW_ADMIN_USER_PASSWORD_AUTH. 
Make sure you uncheck the box for **Generate client secret** as we will not be able to generate an Access Token later if this is enabled.
Press Create App Client, then on the left hand side, press Review and finally Create Pool.
Make a note of the **Pool ID**, **Pool ARN** and **App Client Id**
![Create Client App](doc/CreateClientApp.png)

***IMPORTANT: Make a note of the User Pool ID, you will need it later to run the Sample App***

3) We need a user, so press the Users and Groups tab and then Create User.  Depending on your setup you can activate what you want for the verifications, but we are going to untick all this, name them **MyUser1** . You can leave the password blank, we will set this in the next step. Create this user. 
![Create User](doc/CreateUser.png)

4) Your user will have the default state set to FORCE_CHANGE_PASSWORD which we need to change. Set the password for your user (change the password by another randomly generated string) and make this user permanent:
```
$ aws cognito-idp admin-set-user-password \
    --region eu-west-1 \
    --user-pool-id <POOL_ID> \
    --username "MyUser1" \
    --password '<GENERATED_PASSWORD>' \
    --permanent
```

5) We now need to create an identity pool. In Cognito, press Manage Identity Pools. If you already have one, you'll have to create a new one, otherwise the Create New page will default. Name it **PepperRecognition** and leave the boxes for Unauthenticated Identities and Authenticated Flow Settings unchecked.
In Authentication Providers, select Cognito and copy in the User **Pool ID** and **App Client ID** from earlier.
Press Create Pool.
![Create Identity Pool](doc/CreateIdentityPool.png)
![Create Identity Auth](doc/IdentityPoolAuth.png)

***IMPORTANT: Make a note of the Identity Pool ID, you will need it later to run the Sample App***

6) Use the CLI to retrieve the ARN of your the Rekognition collection you created previously in step **3.4**

```
$ aws rekognition describe-collection \
    --region eu-west-1 \
    --collection-id "PepperRecognition"
```

_Output:_
```
 {
    "FaceCount": 0,
    "FaceModelVersion": "5.0",
    "CollectionARN": "arn:aws:rekognition:eu-west-1:1234567890:collection/PepperRecognition",
    "CreationTimestamp": "2020-11-10T10:43:01.698000+01:00"
}
```

7) On the next page, expand the View Details, and for the Authenticated role, name it **PepperRecognitionAuthenticatedRole** and paste the following, replacing the ARN with the one you just retrieved:

```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "rekognition:SearchFacesByImage",
        "rekognition:IndexFaces"
      ],
      "Resource": [
        "arn:aws:rekognition:eu-west-1:1234567890:collection/PepperRecognition"
      ]
    }
  ]
}
```

![Identity Pool Role](doc/IdentityPoolRole.png)

8) For the Unauthenticated role, name it **PepperRecognitionUnauthenticatedRole** and paste the following: 
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Action": ["*"],
      "Resource": ["*"]
    }
  ]
}
```

Note: If you need to modify these roles in future, go to the IAM service and find them in the Roles tab.

You should be done. Go back to the main README and continue at step **3.6**.