package edu.umich.auth.cosign.util;

import java.util.Vector;
import java.util.Enumeration;

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
    private boolean hasQs=false;
    private boolean hasResource=false;
    Vector factors;

    public ServiceConfig() {
        super();
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

    public boolean hasQs() {
        return hasQs;
    }

    public boolean hasResource() {
        return hasResource;
    }
}
