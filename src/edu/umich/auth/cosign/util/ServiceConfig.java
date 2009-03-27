package edu.umich.auth.cosign.util;

import java.util.*;
import java.util.regex.*;

/**
 <service name="service1">
    <factors>
      <factor></factor>
    </factors>
    <protected>/protected/here</protected>
    <protected>/protected/here/there</protected>
    <protected allowpublicaccess="true">/openhere</protected>
 </service>

 */
public class ServiceConfig {
    private String name;
    private String path;
    private boolean publicAccess=false;
    private String resource;
    private String qs;
    private String validRegEx;
    private boolean hasQs=false;
    private boolean doProxies=false;
    private boolean hasVerify=false;
    //private boolean hasQs=false;
    private boolean hasResource=false;
    private Vector factors;
    private String validationPath;

    public ServiceConfig() {
        super();
        validationPath=null;
        factors = new Vector();
    }

    public void addFactor(String factor){
        this.factors.add(factor);
    }


    public void removeFactors(){
        this.factors.clear();
    }

    public void setFactors(Vector v){
        this.factors = v;
    }

    public Vector getFactors(){
        return factors;
    }

    public String factorsAsString(){
        if(hasFactors()){
            Enumeration e = getFactors().elements();
            StringBuffer strBuff = new StringBuffer();
            while(e.hasMoreElements()){
                strBuff.append((String)e.nextElement());
                if(e.hasMoreElements())
                    strBuff.append(",");
            }
            return strBuff.toString();
        }
        else
            return new String();
    }

    public boolean  hasFactors(){

        return !this.factors.isEmpty();
    }

    public boolean containsFactor(String factor){
       Enumeration e = this.factors.elements();
       while(e.hasMoreElements()){
           String fact = (String) e.nextElement();
           if(fact.equalsIgnoreCase(factor))
               return true;
       }
       return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setResource(String resource) {
        this.resource = resource;
        if(resource!=null && resource.length()>0)
            setHasResource(true);
    }

    public void setQs(String qs) {
        this.qs = qs;
        if(qs!=null && qs.length()>0)
            setHasQs(true);
    }

    private void setHasQs(boolean hasQs) {
        this.hasQs = hasQs;
    }

    private void setHasResource(boolean hasResource) {
        this.hasResource = hasResource;
    }

    public void setValidationPath(String validationPath) {
        this.validationPath = validationPath;
        hasVerify = true;
    }

    public void setValidRegEx(String validRegEx) {
        this.validRegEx = validRegEx;
    }

    public void setDoProxies(boolean doProxies) {
        this.doProxies = doProxies;
    }

    public void setPublicAccess(String publicAccess) {
        if (publicAccess.equalsIgnoreCase("true")) {
            this.publicAccess = true;
        } else {
            this.publicAccess = false;
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public String getResource() {
        return resource;
    }

    public String getQs() {
        return qs;
    }

    public String getValidationPath() {
        return validationPath;
    }



    public String getValidRegEx() {
        return validRegEx;
    }

    public boolean isDoProxies() {
        return doProxies;
    }

    public boolean hasVerify() {
        return hasVerify;
    }

    public boolean hasQs() {
        return hasQs;
    }

    public boolean hasResource() {
        return hasResource;
    }

    /**
     * isLocationUrl
     *
     * @return boolean
     */
    public boolean isLocationUrl() {
        Pattern pattern = Pattern.compile(validRegEx);
        Matcher matcher = pattern.matcher(this.path);

      if(matcher.matches()){
          return true;
      }
        return false;
    }
}
/*Copyright (c) 2002-2008 Regents of The University of Michigan.
All Rights Reserved.

    Permission to use, copy, modify, and distribute this software and
    its documentation for any purpose and without fee is hereby granted,
    provided that the above copyright notice appears in all copies and
    that both that copyright notice and this permission notice appear
    in supporting documentation, and that the name of The University
    of Michigan not be used in advertising or publicity pertaining to
    distribution of the software without specific, written prior
    permission. This software is supplied as is without expressed or
    implied warranties of any kind.

The University of Michigan
c/o UM Webmaster Team
Arbor Lakes
Ann Arbor, MI  48105
*/