package edu.umich.auth.cosign.util;

import javax.security.auth.callback.*;
import java.util.Vector;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class FactorInputCallBack implements Callback {
private Vector factors;

    public FactorInputCallBack() {
    }

    public void setFactors(Vector factors){
        this.factors = factors;
    }

    public Vector getFactors(){
        return factors;
    }

}
