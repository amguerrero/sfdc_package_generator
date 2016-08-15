# Salesforce.com package.xml generator 

## Usage
`groovy GeneratePackage.groovy [path/to/config.json] srcDirectory/ path/to/package.xml`

This command will use the Salesforce.com deployment package structure in the directory *srcDirectory/* and will generate a **package.xml** file in *path/to/package.xml* based on the configuration found in *path/to/config.json*, which is optional. In case *path/to/config.json* is not provided, the script will look for the file **conf/config.json**.

The config file defines what sub-directories in *srcDirectory/* will be taking into account to build the **package.xml** and how the different package.xml types are created, i.e. by using asterisk or using the content of the sub-directories to generate the members of the type tag.

## Example of Use
This script can be useful to deploy to Salesforce.com just the changes in the metadata done after last deployment. If in your company you keep the metadata development in git, and after every deployment there is a tag that identifies the code that has been deployed, i.e: v1.23.

In this scenario, you can run the following commands to generate a delta deployment: 
```
mkdir delta
git diff-tree --no-commit-id --name-only -r HEAD v1.23 | xargs -I {} cp --parents {} delta/
```

This command would create a Salesforce.com delta deployment in *delta/src/* including only the files changed between the the last deployment (tagged with v1.23) and the HEAD of the current branch. With that we can run the the script to obtain the **package.xml** file:
```
groovy GeneratePackage.groovy delta/src delta/src/package.xml
```

This will generate the package.xml file in delta/src/ and now we just need to deploy it, maybe with ant.
