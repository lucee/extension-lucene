<cfcomponent accessors="true" >
	
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
		updates a collection and adds key to the index
		*/
		public void function update(){
			this.setAttributes(argumentCollection=arguments);
			variables.attributes.action = "update";
			return invokeTag();
		}

		/*
		deletes all of the documents in a collection.Causes the collection to be taken offline, preventing searches.
		*/

		public void function purge(){
			this.setAttributes(argumentCollection=arguments);
			variables.attributes.action = "purge";
			return invokeTag();
		}

		/*
		removes collection documents as specified by the key attribute.
		*/

		public void function delete(){
			this.setAttributes(argumentCollection=arguments);
			variables.attributes.action = "delete";
			return invokeTag();
		}

		/*
		deletes all of the documents in a collection, and then performs an update
		*/

		public void function refresh(){
			this.setAttributes(argumentCollection=arguments);
			variables.attributes.action = "refresh";
			return invokeTag();
		}

		/*
		Returns a query result set, of indexed data .
		*/

		public query function list(){
			this.setAttributes(argumentCollection=arguments);
			variables.attributes.action = "list";
			return invokeTag();
		}

		public function setAttributes() {
			StructAppend(variables.attributes, arguments, true);
			return this;
		}
	</cfscript>

	<cffunction name="invokeTag" output="true" access="private" returntype="any" hint="invokes the service tag">
			<cfset tagAttributes = variables.attributes>
			<cfif tagAttributes.action eq 'list'>
				<cfset tagAttributes.name = 'name'>
			</cfif>

			<cfindex attributeCollection="#tagAttributes#">

			<cfswitch expression="#tagAttributes.action#">

				<cfcase value="list">

					<!--- <cfset result = {
						name = name
					}> --->
					<cfreturn name>
				</cfcase>

			</cfswitch>
			<cfreturn "">
	</cffunction>
</cfcomponent>
