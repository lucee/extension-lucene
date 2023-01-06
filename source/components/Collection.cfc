<cfcomponent  accessors="true">
	<cfscript>
	variables.attributes = {};

	/**
	 * @hint Constructor
	*/
	public function init(){
		setAttributes(argumentCollection=arguments);
		return this;
	}

	/*
	registers the collection with lucee.
	*/
	public void function create(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "create";
		return invokeTag();
	}

	/*
	repair
	*/

	public void function repair(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "repair";
		return invokeTag();
	}

	/*
	unregisters a collection and deletes its directories.
	*/

	public void function delete(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "delete";
		return invokeTag();
	}

	/*
	optimizes the structure and contents of the collection for searching; recovers space. Causes collection to be taken offline, preventing searches and indexing.
	*/

	public void function optimize(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "optimize";
		return invokeTag();
	}

	/*
	Returns a query result set, named from the name attribute value, of the attributes of the collections that are registered by lucee.
	*/

	public query function list(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "list";
		return invokeTag();
	}

	/*
	creates a map to a collection.
	*/

	public void function map(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "map";
		return invokeTag();
	}

	/*
	Retrieves categories from the collection and indicates how many documents are in each one. Returns a structure of structures in which the category representing each substructure is associated with a number of documents
	*/

	public Struct function categorylist(){
		this.setAttributes(argumentCollection=arguments);
		variables.attributes.action = "categorylist";
		return invokeTag();
	}

	public function setAttributes() {
		StructAppend(variables.attributes, arguments, true);
		return this;
	}

	</cfscript>

	<cffunction name="invokeTag" output="true" access="private" returntype="any" hint="invokes the service tag">
			<cfset tagAttributes = variables.attributes>
			<cfif tagAttributes.action eq 'list' || tagAttributes.action eq 'categorylist'>
				<cfset tagAttributes.name = 'name'>
			</cfif>

			<!--- the xmlvar is forced for both actions --->
			<cfset tagAttributes.xmlvar = 'xmlvar'>

			<cfcollection attributeCollection="#tagAttributes#">

			<cfswitch expression="#tagAttributes.action#">

				<cfcase value="list">

					<!--- <cfset result = {
						name = name
					}> --->
					<cfreturn name>

				</cfcase>

				<cfcase value="categorylist">

					<cfreturn name>
				</cfcase>

			</cfswitch>
			<cfreturn "">
	</cffunction>

</cfcomponent>
