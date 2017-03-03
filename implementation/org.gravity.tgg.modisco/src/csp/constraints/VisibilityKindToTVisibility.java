package csp.constraints;

import org.eclipse.gmt.modisco.java.VisibilityKind;
import org.gravity.typegraph.basic.TVisibility;
import org.moflon.tgg.language.csp.Variable;
import org.moflon.tgg.language.csp.impl.TGGConstraintImpl;

public class VisibilityKindToTVisibility extends TGGConstraintImpl {
	
	
	private VisibilityKind TVisibilityToVKind(TVisibility tVisibility){		
		if(tVisibility == TVisibility.TPUBLIC){
			return VisibilityKind.PUBLIC;
		}
		if(tVisibility == TVisibility.TPROTECTED){
			return VisibilityKind.PROTECTED;
		}
		if(tVisibility == TVisibility.TPACKAGE){
			return VisibilityKind.NONE;
		}
		if(tVisibility == TVisibility.TPRIVATE){
			return VisibilityKind.PRIVATE;
		}	
		// default Value
		return VisibilityKind.NONE;
	}
	
	
	private TVisibility VKindToTVisibility(VisibilityKind vKind){		
		if(vKind == VisibilityKind.PUBLIC){
			return TVisibility.TPUBLIC;
		}
		if(vKind == VisibilityKind.PROTECTED){
			return TVisibility.TPROTECTED;
		}
		if(vKind == VisibilityKind.NONE){
			return TVisibility.TPACKAGE;
		}
		if(vKind == VisibilityKind.PRIVATE){
			return TVisibility.TPRIVATE;
		}	
		// default Value
		return TVisibility.TPACKAGE;
	}
	
	public void solve(Variable var_0, Variable var_1){
    	String bindingStates = getBindingStates(var_0, var_1);
    	
    	switch(bindingStates){
    	case "BF":
    		VisibilityKind vKind = (VisibilityKind) var_0.getValue();
    		var_1.bindToValue(VKindToTVisibility(vKind));
    		setSatisfied(true);
    		break;
    	case "FB":
    		TVisibility tVisibility = (TVisibility) var_1.getValue();
    		var_0.bindToValue(TVisibilityToVKind(tVisibility));
    		setSatisfied(true);
    		break;
    	case "BB":
    		TVisibility tVis = (TVisibility) var_1.getValue();
    		VisibilityKind visKind = (VisibilityKind) var_0.getValue();
    		setSatisfied(visKind.equals(TVisibilityToVKind(tVis)));
    		break;
    	default: 
    		setSatisfied(true);
    	}
    	
  	}	  
}