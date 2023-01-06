<cfcomponent accessors="true">
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
		Executes searches against data indexed
		*/
		public Query function search(){
			this.setAttributes(argumentCollection=arguments);
			return invokeTag();
		}

		public function setAttributes() {
			StructAppend(variables.attributes, arguments, true);
			return this;
		}
	</cfscript>
	<cffunction name="invokeTag" output="true" access="private" returntype="any" hint="invokes the service tag">
			<cfset tagAttributes = variables.attributes>
			<cfset tagAttributes.name = 'name'>
			<cfsearch attributeCollection="#tagAttributes#">
			<cfreturn name>
	</cffunction>
</cfcomponent>
