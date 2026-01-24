/**
 * 
 */
/**
 * 
 */
module ingegneriaSoftware4 {
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
	requires com.fasterxml.jackson.annotation;
    opens ingegneriaSoftware to com.fasterxml.jackson.databind;
}