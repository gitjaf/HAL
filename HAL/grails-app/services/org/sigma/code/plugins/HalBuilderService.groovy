package org.sigma.code.plugins

import grails.converters.JSON
import grails.web.JSONBuilder;

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.mapping.LinkGenerator


class HalBuilderService {
	
	//TODO Cambiar el termino resource por representation donde corresponda.

	def excludedProperties = ['class', 'metaClass', 'halRepresenter' ]

	def grailsApplication

	def linksParams = [append: "", prepend: "", query:""]
	
	LinkGenerator grailsLinkGenerator
	
	
	def buildModel = {item ->
		HashMap links = new HashMap()
		HashMap resource = new HashMap()
		HashMap embeddeds = new HashMap()

		def gdc = (grailsApplication.getDomainClass(item.getClass().getName())) ?:
				grailsApplication.getDomainClass((item.class.getName()[0..<item.class.getName().indexOf("_\$")])) 
		if(!gdc){println item.class.getName()}
		links.self = [href:this.getLinkFor(item)]
		resource."id" = item.id
		item.properties.each{name, value ->
		
			if(value && !(name in excludedProperties) && !(name ==~ /.*Id$/) ){
				def pp = gdc.persistentProperties.find{it.name == name}
				
				if(pp?.association){
					links."${name}" = this.getAssociation(pp, value, false)  
					if(pp.name in item?.halRepresenter?.embedded){
						 embeddeds.put("$name",this.getAssociation(pp, value, true))
					} 
				} else {
					resource."${name}" = value
				}
			}
		}
		if(!embeddeds.isEmpty()){
			resource._embedded = embeddeds
		}
		resource._links = links
		return resource
	}
	
    def buildModelList = { List rawData, Map linksParams ->
			
		if(linksParams){ this.linksParams = linksParams}

		List resources = new ArrayList() 
		
		rawData.each{ 
			
			resources.add(buildModel(it))
		}
		
		return resources
	}
	
	protected getAssociation = { GrailsDomainClassProperty pp, inst, embedded ->
		if(pp.isOneToMany() || pp.isManyToMany()){
			return ((embedded) ? getEmbeddedFor(inst as List) : inst.collect{["href": getLinkFor(it)]})
		} else {
			return ((embedded) ? getEmbeddedFor(inst) : ["href":getLinkFor(inst)])
		}	
	}
	
	protected getLinkFor = { inst ->
		def controller = getController(inst)
		def action = getAction(inst, "show")
		
		return getLinkTo(controller, action, inst.id, this.linksParams)
			
	}
	
	protected getController = { inst ->
		return (inst.properties?.halRepresenter?.controller ?: 
			(!grailsApplication.getDomainClass(inst.getClass().getName())) ?
				inst.class.getSimpleName().toLowerCase()[0..<inst.class.getSimpleName().indexOf("_\$")] :
			 	inst.class.getSimpleName().toLowerCase())
	}
	
	protected getAction = { inst, String action ->
		return (inst.properties?.halRepresenter?."${action}" ?:	action)
	}
	
	protected getLinkTo(String con, String act, linksParams){
		return linksParams.prepend + grailsLinkGenerator.link(controller: con, action: act,  absolute: false) - grailsLinkGenerator.contextPath - "/$act" + linksParams.append
	}
	
	protected getLinkTo(String con, String act, Long id, linksParams){
		return linksParams.prepend + grailsLinkGenerator.link(controller: con, action: act, id: id, absolute: false) - grailsLinkGenerator.contextPath - "/$act" + linksParams.append
	}	

	protected getEmbeddedFor(List list) { 
		return buildModelList(list, this.linksParams)
	}
	
	protected getEmbeddedFor = { inst ->
		return buildModel(inst)
	}
	
	
}
