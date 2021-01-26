# AWS CLI Instructions

### Create an Amazon Cognito identity pool, to configure access to the Amazon Recognition service

If you are comfortable with the CLI and AWS, follow these steps. Otherwise we recommend you follow the GUI instructions linked previously.

Preface: Retrieve the ARN of your the Rekognition collection you created previously:

```
$ aws rekognition describe-collection \
    --region eu-west-1 \
    --collection-id "PepperRecognition"
```

*Output:*
```
{
    "FaceCount": 0,
    "FaceModelVersion": "5.0",
    "CollectionARN": "arn:aws:rekognition:eu-west-1:1234567890:collection/PepperRecognition",
    "CreationTimestamp": "2020-11-10T10:43:01.698000+01:00"
}
```

Here the ARN is `arn:aws:rekognition:eu-west-1:1234567890:collection/PepperRecognition`.

1) Create two *policy* documents on your drive. Those documents describe what can an authenticated user can do (access our collection) and what an unauthenticated user can do (nothing). In the following commands, replace the collection ARN by the one you obtained with the command at step 1):

```
# Authenticated users can access our collection
$ cat > iam.authenticated.policy <<EOF
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
EOF
```

```
# Unauthenticated users do not access anything
$ cat > iam.unauthenticated.policy <<EOF
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
EOF
```

2) Use these documents to create two IAM policies in the Amazon IAM service (the `file://` part in front of the document name is mandatory in the command):

```
$ aws iam create-policy \
    --region eu-west-1 \
    --policy-name PepperRecognitionAuthenticatedPolicy \
    --policy-document file://iam.authenticated.policy
```

*Output:*
```
{
    "Policy": {
        "PolicyName": "PepperRecognitionAuthenticatedPolicy",
        "PolicyId": "AAABBBCCCDDDEEEFFFGGG",
        "Arn": "arn:aws:iam::1234567890:policy/PepperRecognitionAuthenticatedPolicy",
        "Path": "/",
        "DefaultVersionId": "v1",
        "AttachmentCount": 0,
        "PermissionsBoundaryUsageCount": 0,
        "IsAttachable": true,
        "CreateDate": "2020-11-12T15:28:18+00:00",
        "UpdateDate": "2020-11-12T15:28:18+00:00"
    }
}
```

```
$ aws iam create-policy \
    --region eu-west-1 \
    --policy-name PepperRecognitionUnauthenticatedPolicy \
    --policy-document file://iam.unauthenticated.policy
```

*Output:*
```
{
    "Policy": {
        "PolicyName": "PepperRecognitionUnauthenticatedPolicy",
        "PolicyId": "XXXBBBCCCDDDEEEFFFGGG",
        "Arn": "arn:aws:iam::1234567890:policy/PepperRecognitionUnauthenticatedPolicy",
        "Path": "/",
        "DefaultVersionId": "v1",
        "AttachmentCount": 0,
        "PermissionsBoundaryUsageCount": 0,
        "IsAttachable": true,
        "CreateDate": "2020-11-12T15:28:36+00:00",
        "UpdateDate": "2020-11-12T15:28:36+00:00"
    }
}
```

3) Then create an Identity Pool:
```
$ aws cognito-identity create-identity-pool \
    --region eu-west-1 \
    --identity-pool-name "PepperRecognition" \
    --no-allow-unauthenticated-identities \
    --no-allow-classic-flow
```

*Output:*
```
{
    "IdentityPoolId": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb",
    "IdentityPoolName": "PepperRecognition",
    "AllowUnauthenticatedIdentities": false,
    "AllowClassicFlow": false,
    "IdentityPoolTags": {}
}
```


***IMPORTANT: Make a note of the Identity Pool ID, you will need it later to run the Sample App***

4) Again create two policy documents on your drive. These policy will be used to create roles. Replace the identity pool id by the one you obtained at step 4):
```
$ cat > role.authenticated.policy <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "cognito-identity.amazonaws.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "cognito-identity.amazonaws.com:aud": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb"
                },
                "ForAnyValue:StringLike": {
                    "cognito-identity.amazonaws.com:amr": "authenticated"
                }
            }
        }
    ]
}
EOF
```

```
$ cat > role.unauthenticated.policy <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "cognito-identity.amazonaws.com"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringEquals": {
                    "cognito-identity.amazonaws.com:aud": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb"
                },
                "ForAnyValue:StringLike": {
                    "cognito-identity.amazonaws.com:amr": "unauthenticated"
                }
            }
        }
    ]
}
EOF
```

5) Create two roles, one authenticated, one unauthenticated:
```
$ aws iam create-role \
    --region eu-west-1 \
    --role-name PepperRecognitionAuthenticatedRole \
    --assume-role-policy-document file://role.authenticated.policy
```

*Output:*
```
{
    "Role": {
        "Path": "/",
        "RoleName": "PepperRecognitionAuthenticatedRole",
        "RoleId": "AAABBBCCCAAABBBCCCEEE",
        "Arn": "arn:aws:iam::1234567890:role/PepperRecognitionAuthenticatedRole",
        "CreateDate": "2020-11-12T15:31:53+00:00",
        "AssumeRolePolicyDocument": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {
                        "Federated": "cognito-identity.amazonaws.com"
                    },
                    "Action": "sts:AssumeRoleWithWebIdentity",
                    "Condition": {
                        "StringEquals": {
                            "cognito-identity.amazonaws.com:aud": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb"
                        },
                        "ForAnyValue:StringLike": {
                            "cognito-identity.amazonaws.com:amr": "authenticated"
                        }
                    }
                }
            ]
        }
    }
}
```

```
$ aws iam create-role \
    --region eu-west-1 \
    --role-name PepperRecognitionUnauthenticatedRole \
    --assume-role-policy-document file://role.unauthenticated.policy
```

*Output:*
```
{
    "Role": {
        "Path": "/",
        "RoleName": "PepperRecognitionUnauthenticatedRole",
        "RoleId": "GGGHHHIIIJJJAAABBBCCC",
        "Arn": "arn:aws:iam::1234567890:role/PepperRecognitionUnauthenticatedRole",
        "CreateDate": "2020-11-12T15:32:13+00:00",
        "AssumeRolePolicyDocument": {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": {
                        "Federated": "cognito-identity.amazonaws.com"
                    },
                    "Action": "sts:AssumeRoleWithWebIdentity",
                    "Condition": {
                        "StringEquals": {
                            "cognito-identity.amazonaws.com:aud": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb"
                        },
                        "ForAnyValue:StringLike": {
                            "cognito-identity.amazonaws.com:amr": "unauthenticated"
                        }
                    }
                }
            ]
        }
    }
}
```

Note the Arn of the roles (here `arn:aws:iam::1234567890:role/PepperRecognitionAuthenticatedRole` and `arn:aws:iam::1234567890:role/PepperRecognitionUnauthenticatedRole`)

6) Attach the policies from step 3) to the roles. Attach the authenticated policy to the authenticated role, and the unauthenticated policy to the unauthenticated role:
```
$ aws iam attach-role-policy \
    --region eu-west-1 \
    --role-name PepperRecognitionAuthenticatedRole \
    --policy-arn 'arn:aws:iam::1234567890:policy/PepperRecognitionAuthenticatedPolicy'
```

```
$ aws iam attach-role-policy \
    --region eu-west-1 \
    --role-name PepperRecognitionUnauthenticatedRole \
        --policy-arn 'arn:aws:iam::1234567890:policy/PepperRecognitionUnauthenticatedPolicy'
```

7) Finally attach the roles to the identity pool

```
$ aws cognito-identity set-identity-pool-roles \
    --region eu-west-1 \
    --identity-pool-id "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb" \
    --roles authenticated="arn:aws:iam::1234567890:role/PepperRecognitionAuthenticatedRole",unauthenticated="arn:aws:iam::1234567890:role/PepperRecognitionUnauthenticatedRole"
```

Now authenticated users can send request to our AWS rekognition collection.
One last thing is missing: we need a way to authenticate users!

### Create an Amazon Cognito User Pool to define a user and its login informations

There are many ways to authenticate users with Amazon. You can use custom authentication, or define login password in Amazon Cognito, or use federated identity like Google or Facebook.
We chose to set login and passwords directly in Amazon Cognito. And to simplify things a little bit, we will directly get authentication tokens using AWS cli, and copy paste them in our sample app, so that you won't have to login in the Android sample app on Pepper.

Follow these steps:

1) Create a user pool that will contains your users:
```
`$ aws cognito-idp create-user-pool --region eu-west-1 --pool-name "PepperRecognitionUsers"`
```

*Output:*
```
{
    "UserPool": {
        "Id": "eu-west-1_AA1122aab",
        "Name": "PepperRecognitionUsers",
        ....

        "Arn": "arn:aws:cognito-idp:eu-west-1:1234567890:userpool/eu-west-1_AA1122aab"
    }
}
```

Save the user pool id (here `eu-west-1_AA1122aab`)
***IMPORTANT: Make a note of the User Pool ID, you will need it later to run the Sample App***

2) Add a user to your pool:
```
$ aws cognito-idp admin-create-user \
    --region eu-west-1 \
    --user-pool-id eu-west-1_AA1122aab \
    --username "MyUser1"
```

3) Set the password for your user (change the password by another randomly generated string):
```
$ aws cognito-idp admin-set-user-password \
    --region eu-west-1 \
    --user-pool-id eu-west-1_AA1122aab \
    --username "MyUser1" \
    --password '<GENERATED_PASSWORD>' \
    --permanent
```

4) Create a client so that we can later ask for access tokens using the AWS CLI `admin-initiate-auth` command
```
$ aws cognito-idp create-user-pool-client \
    --region eu-west-1 \
    --user-pool-id eu-west-1_AA1122aab \
    --client-name PepperRekognitionLoginApp \
    --explicit-auth-flows ALLOW_ADMIN_USER_PASSWORD_AUTH ALLOW_REFRESH_TOKEN_AUTH \
    --id-token-validity 24
```

*Output:*
```
{
    "UserPoolClient": {
        "UserPoolId": "eu-west-1_AA1122aab",
        "ClientName": "PepperRekognitionLoginApp",
        "ClientId": "2hhhiiijjj111appclientidxx",
        "LastModifiedDate": "2020-11-12T17:34:24.124000+01:00",
        "CreationDate": "2020-11-12T17:34:24.124000+01:00",
        "RefreshTokenValidity": 30,
        "IdTokenValidity": 24,
        "TokenValidityUnits": {},
        "ExplicitAuthFlows": [
            "ALLOW_ADMIN_USER_PASSWORD_AUTH",
            "ALLOW_REFRESH_TOKEN_AUTH"
        ],
        "AllowedOAuthFlowsUserPoolClient": false
    }
}
```

Remember the ClientId (here `2hhhiiijjj111appclientidxx`) for the next section.

5) Link the user pool with the identity pool:
```
$ aws cognito-identity update-identity-pool \
    --region eu-west-1 \
    --identity-pool-id "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb" \
    --identity-pool-name "PepperRecognition" \
    --no-allow-unauthenticated-identities \
    --cognito-identity-providers ProviderName="cognito-idp.eu-west-1.amazonaws.com/eu-west-1_AA1122aab",ClientId="2hhhiiijjj111appclientidxx"
```

*Output:*
```
{
    "IdentityPoolId": "eu-west-1:aaa111bb-aa11-aa11-aa11-aaaa1111bbbb",
    "IdentityPoolName": "PepperRecognition",
    "AllowUnauthenticatedIdentities": false,
    "AllowClassicFlow": false,
    "CognitoIdentityProviders": [
        {
            "ProviderName": "cognito-idp.eu-west-1.amazonaws.com/eu-west-1_AA1122aab",
            "ClientId": "2hhhiiijjj111appclientidxx",
            "ServerSideTokenCheck": false
        }
    ],
    "IdentityPoolTags": {}
}
```

You should be done. Go back to the main README and continue at step **3.6**.