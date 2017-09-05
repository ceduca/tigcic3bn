#!/usr/bin/python
# -*- coding: utf-8 -*-
'''
Created on May 7, 2016

@author: hasan
'''

from extractor.entities import Profile
import os
import sys

'''
Created on May 7, 2016

@author: hasan
'''

def convertHtmlToClass(strDir):
    arrObjsPerfis = []
    dirs = os.listdir( strDir )
    for strFile in dirs:
        strCompFile = strDir+"/"+strFile
        if os.path.isfile(strCompFile):
            with open(strCompFile, 'r') as f:
                read_data = f.read()
                arrObjsPerfis.append(Profile.manager.getProfileFromHtml(read_data))
    return arrObjsPerfis

def writeTXTProfiles(strOut,arrObjProfiles):
    
    def adicionaPositions(arrLine,arrPosition,intMax,isCurrent):
            #adiciona todas as posicoes atuais 
            for objPosition in arrPosition:
                arrLine.append(objPosition.strCompanyName)
                arrLine.append(objPosition.strJobTitle)
                arrLine.append(str(objPosition.intFromYear))
                if(not isCurrent):
                    arrLine.append(str(objPosition.intToYear))
                
            #caso tenha faltado algum, adiciona vazio
            for idx in range(intMax-len(arrPosition)):
                arrLine.append("")
                arrLine.append("")
                arrLine.append("")
                if(not isCurrent):
                    arrLine.append("")
    
    #verifica quantos "pastPositions" existem
    intMaxCurPos = 0
    for objProfile in arrObjProfiles:
        if len(objProfile.arrCurrentPositions) > intMaxCurPos:
            intMaxCurPos = len(objProfile.arrCurrentPositions)
    #verifica quantos "pastpositions" existem
    intMaxPrevPos = 0
    for objProfile in arrObjProfiles:
        if len(objProfile.arrPastPositions) > intMaxPrevPos:
            intMaxPrevPos = len(objProfile.arrPastPositions)
    
    #agrupa skills e os indexa
    idxPerSkill = {}
    arrSkillPerIdx =[]
    intLasIdx = -1
    
    for objProfile in arrObjProfiles:
        for strSkill in objProfile.arrSkills:
            #caso seja uma nova habilidade, atribua um indice a ela
            if strSkill not in idxPerSkill:
                intLasIdx = intLasIdx+1
                idxPerSkill[strSkill] = intLasIdx
                arrSkillPerIdx.append(strSkill)
                

    
    #escreve arquivo texto com todas os profiles
    with open(strOut, 'w') as f:
        
        
        ##Cabecalho###
        arrCabecalho = ["cidade"]
        #emprego atual
        if intMaxCurPos != 1:
            for idx in range(intMaxCurPos):
                arrCabecalho.append("current_position_jobTitle_"+str(idx))
                arrCabecalho.append("current_position_company_"+str(idx))
                arrCabecalho.append("current_position_since_"+str(idx))
        else:
            arrCabecalho.append("current_position_jobTitle")
            arrCabecalho.append("current_position_company")
            arrCabecalho.append("current_position_since")
            
        #empregos passados
        for idx in range(intMaxPrevPos):
                arrCabecalho.append("past_position_jobTitle_"+str(idx))
                arrCabecalho.append("past_position_company_"+str(idx))
                arrCabecalho.append("past_position_from_"+str(idx))        
                arrCabecalho.append("past_position_to_"+str(idx))
                
        #habilidades 
        for idx,strSkill in enumerate(arrSkillPerIdx):
            arrCabecalho.append(strSkill)
        f.write(";".join(arrCabecalho))
        f.write("\n")
        
        ##linhas##
        for objProfile in arrObjProfiles:
            arrLine = []
            arrLine.append(objProfile.strCity)
            
            #adiciona positions
            adicionaPositions(arrLine,objProfile.arrCurrentPositions,intMaxCurPos,True)
            adicionaPositions(arrLine,objProfile.arrPastPositions,intMaxPrevPos,False)
            
            #adiciona skills
            for idx,strSkill in enumerate(arrSkillPerIdx):
                strExistSkill = "1" if strSkill in objProfile.arrSkills else "0"
                arrLine.append(strExistSkill) 
                     
            f.write(";".join(arrLine))
            f.write("\n")
if __name__ == '__main__':
    #print "oioi"
    strDir = "/home/hasan/webLinkedin/"
    #strFile = strDir+"Daniel Hasan Dalip _ LinkedIn.html"
    arrObjPerfis = convertHtmlToClass(strDir)
    print arrObjPerfis[4]
    writeTXTProfiles("/home/hasan/perfis.txt",arrObjPerfis)