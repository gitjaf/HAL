package org.sigma.code.plugins

import grails.converters.JSON
import grails.web.JSONBuilder;

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.mapping.LinkGenerator


class HalBuilderService {

	def excludedProperties = ['class', 'metaClass', 'id', 'halRepresenter' ]

	def grailsApplication
	
	LinkGenerator grailsLinkGenerator
	
	
	def build = {item ->
		HashMap links = new HashMap()
		HashMap resource = new HashMap()

		def gdc = grailsApplication.getDomainClass(item.class.getName())
		println item
		links.self = [href:this.getLinkFor(item)]
		
		item.properties.each{name, value ->
			if(value && !(name in excludedProperties)  && !(name ==~ /.*Id$/) ){
				def pp = gdc.persistentProperties.find{it.name == name}
				if(pp?.association){
					if(pp.name in item?.halRepresenter?.embedded){
						resource._embedded = ["$name": this.getAssociation(pp, value, true)]
					} else {
						links."${name}" = [href: this.getAssociation(pp, item, false)]  
					}
				} else {
					resource."${name}" = value
				}
			}
		}
		resource._links = links
		
		return resource
	}
	
    def buildList = { List rawData ->
	
		List resources = new ArrayList() 
		
		rawData.each{ 
			resources.add(build(it))
		}
		
		return resources
	}
	
	protected getAssociation = { GrailsDomainClassProperty pp, inst, embedded ->
		if(pp.isOneToMany() || pp.isManyToMany()){
			return ((embedded) ? getEmbeddedFor(inst as List) : getLinkTo(getController(pp.type.getName()), getAction(inst, "list")))
		} else {
			return ((embedded) ? getEmbeddedFor(inst) : getLinkFor(inst."${pp.name}"))
		}	
	}
	
	protected getLinkFor = { inst ->
		def controller = getController(inst)
		def action = getAction(inst, "show")
		
		return getLinkTo(controller, action, inst.id)
			
	}
	
	protected getController = { inst ->
		return (inst.properties?.halRepresenter?.controller ? inst.properties.halRepresenter.controller : inst.class.getSimpleName())
	}
	
	protected getAction = { inst, String action ->
		return (inst.properties?.halRepresenter?."${action}" ? inst.properties.halRepresenter."${action}" :
			grailsApplication.config.halRepresenter."${action}" ? grailsApplication.config.halRepresenter."${action}" : action)
	}
	
	protected getLinkTo(String con, String act){
		return grailsLinkGenerator.link(controller: con, action: act, absolute: true)
	}
	
	protected getLinkTo(String con, String act, Long id){
		return grailsLinkGenerator.link(controller: con, action: act, id: id, absolute: true)
	}	

	protected getEmbeddedFor(List list) { 
		return buildList(list)
	}
	
	protected getEmbeddedFor = { inst ->
		return build(inst)
	}
	
	
}
