module ingegneriaSoftware4 {

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.annotation;

    opens ingegneriaSoftware.domain to com.fasterxml.jackson.databind;
    opens ingegneriaSoftware.controller to com.fasterxml.jackson.databind;
    opens ingegneriaSoftware.domain.dati to com.fasterxml.jackson.databind;

}
