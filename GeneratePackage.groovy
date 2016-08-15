import groovy.json.JsonSlurperClassic

// #### MAIN ####
def (configFile, srcDirName, packageFileName) = parseArgs(args)
def packageTemplate = '''
<Package xmlns="http://soap.sforce.com/2006/04/metadata">
    <version></version>
</Package>
'''

def config = new JsonSlurperClassic().parse(new File(configFile))

def xmlParser = new XmlParser(false, true, true)
def packageXml = xmlParser.parseText(packageTemplate)

println "Finding directories and files to create package"
def srcDir = new File(srcDirName)
srcDir.eachDir() { dir ->
	if (config.dirs."${dir.name}") {
		def nodeConfig = config.dirs."${dir.name}"

		println " * Found ${dir.name}: adding ${nodeConfig.xmlTag}"
		def types = typesNode(xmlParser)
		if (nodeConfig.acceptsAsterisk) {
			types.append membersNode(xmlParser, "*")
		} else {
			if (nodeConfig.extension) {
				addMembers(xmlParser, types, dir, nodeConfig.extension)
			} else {
				addMembersExcludePattern(xmlParser, types, dir, nodeConfig.excludeExtension)
			}
		}
		types.append nameNode(xmlParser, nodeConfig.xmlTag)
		packageXml.append types
	} else {
		println " * '${dir.name}' directory is not configured... It will be ignored"
	}
}

packageXml.version[0].value = config.version


println "Sorting package.xml"
packageXml.children().sort(true) {
	def key = (it.name() instanceof groovy.xml.QName) ? it.name().getLocalPart() : it.name()

	key += (it.name[0] == null) ? '#' : "#${it.name[0].value()}"
}

println "Writting package to $packageFileName file"
xmlToFile(packageXml, packageFileName)

// #### FUNCTIONS ####
def addMembersExcludePattern(def parser, def node, def dir, def excludeExtension) {
	dir.eachDir() { subDir ->
		node.append membersNode(parser, subDir.name)

		subDir.eachFile() { file ->
			if (!file.name.endsWith(excludeExtension)) {
				node.append membersNode(parser, subDir.name + "/" + file.name)
			}
		}	
	}
}

def addMembers(def parser, def node, def dir, def extension) {
	dir.eachDir() { subDir ->
		node.append membersNode(parser, subDir.name)

		subDir.eachFileMatch(~/.*${extension}/) { file ->
			node.append membersNode(parser, subDir.name + "/" + (file.name - "$extension"))
		}	
	}
	dir.eachFileMatch(~/.*${extension}/) { file ->
		node.append membersNode(parser, file.name - "$extension")
	}
}

def typesNode(def parser) {
	parser.parseText('<types></types>')
}

def membersNode(def parser, def member) {
	def node = parser.parseText("<members></members>")
	node.value = member

	node
}

def nameNode(def parser, def name) {
	def node = parser.parseText("<name></name>")
	node.value = name

	node
}

def parseArgs(def args) {
	def configFile, srcDir, packageFile
	if (args.size() < 2 || args.size() > 3) {
		println "Usage: groovy GeneratePackage.groovy [<path/to/config.json>] <srcDirectory> <path/to/package.xml>"
		System.exit(-1)
	}


	configFile = (args.size() == 2) ? "conf/config.json" : args[0]
	srcDir = (args.size() == 2) ? args[0] : args[1]
	packageFile = (args.size() == 2) ? args[1] : args[2]

	[configFile, srcDir, packageFile]
}

def xmlToFile(def xml, def fileName) {
	def sw = new StringWriter()
	def printer = new XmlNodePrinter(new PrintWriter(sw), '    ')
	printer.with {
	  preserveWhitespace = true
	  expandEmptyElements = true
	}
	printer.print(xml)

	new File(fileName).withWriter('UTF-8') { it.write "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n$sw" }
}