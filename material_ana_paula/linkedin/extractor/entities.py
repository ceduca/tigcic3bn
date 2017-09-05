#!/usr/bin/python
# -*- coding: utf-8 -*-
'''
Created on May 7, 2016

@author: hasan
'''
from scrapy.selector import Selector
class ProfileManager():
    def getProfileFromHtml(self,strHTML):
        objSel = Selector(text=strHTML)
        #Dados do perfil (cidade)
        strCidade = self.getLocality(objSel)
        #posicao atual
        arrPosAtual = self.getJobPosition(objSel, True)
        
        #posicoes anteriores
        arrPosAnteriores = self.getJobPosition(objSel, False)
        #habilidades
        arrHabilidades = self.getSkillsFromProfile(objSel)

        return Profile(strCidade, arrPosAtual, arrPosAnteriores, arrHabilidades)
    def getJobPosition(self,objSelector,isCurrent):
        def convertYear(strYear):
            
            return strYear[len(strYear)-4:] if strYear != None else None
        def getFirst(arr):
            return arr[0] if len(arr)>0 else ""
        
        strDataSection = "currentPositionsDetails" if isCurrent else "pastPositionsDetails"
        
        arrPositions = []
        #verifica a quantidade de itens
        intQtd = len(objSelector.xpath("//li[@data-section='"+strDataSection+"']"))
        for idx in range(intQtd):
            strPos = str(idx+1)
            #jobTitle
            strJobTitle = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//h4[@class='item-title']/a/text()").extract())
            if not strJobTitle:
                strJobTitle = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//h4[@class='item-title']/text()").extract())
                        
            #empresa 
            strJobCompany = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//h5[@class='item-subtitle']/a/text()").extract())
            if not strJobCompany:
                strJobCompany = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//h5[@class='item-subtitle']/text()").extract())
            #from
            strFrom = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//time[1]/text()").extract())
            
            #to
            strTo = getFirst(objSelector.xpath("//li[@data-section='"+strDataSection+"']["+strPos+"]//time[2]/text()").extract()) if not isCurrent else None
            arrPositions.append(Position(strJobCompany, strJobTitle, convertYear(strFrom), convertYear(strTo)))
            
        return arrPositions
    
    def getSkillsFromProfile(self,objSelector):
        return objSelector.xpath("//li[@class='skill' or @class='skill extra']//span/text()").extract()
    def getLocality(self,objSelector):
        arrLocal = objSelector.xpath("//span[@class='locality']/text()").extract()
        return arrLocal[0] if len(arrLocal)>0 else ""
class Profile(object):
    '''
    classdocs
    '''
    manager = ProfileManager()

    def __init__(self,strCity,arrCurrentPositions,arrPastPositions,arrSkills):
        self.strCity = strCity
        self.arrCurrentPositions = arrCurrentPositions
        self.arrPastPositions = arrPastPositions
        self.arrSkills = set(arrSkills)
    
    def __str__(self):
        return "Cidade: "+self.strCity+"\nHabilidades:"+str(self.arrSkills)+"\nEmprego atual:"+str(self.arrCurrentPositions)+"\nEmpregos Anteriores: "+str(self.arrPastPositions)
    def __repr__(self):
        return self.__str__()
class Position(object):
    def __init__(self,strCompanyName,strJobTitle,intFromYear,intToYear):
        self.strCompanyName = strCompanyName
        self.strJobTitle = strJobTitle
        self.intFromYear = intFromYear
        self.intToYear = intToYear
    
    def __str__(self):
        strPeriodo = "de "+str(self.intFromYear)+" até "+str(self.intToYear) if self.intToYear != None else "desde "+str(self.intFromYear) 
        return self.strCompanyName+"("+self.strJobTitle+") "+strPeriodo
    
    def __repr__(self):
        return self.__str__()