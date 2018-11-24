import com.fasterxml.jackson.core.JsonFactory;

module com.fasterxml.jackson.core {
	exports com.fasterxml.jackson.core;



	exports com.fasterxml.jackson.core.type to com.fasterxml.jackson.databind, com.jwebmp.core;
	exports com.fasterxml.jackson.core.io to com.fasterxml.jackson.databind, com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8;
	exports com.fasterxml.jackson.core.util to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.base  to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.json  to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.filter to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.format  to com.fasterxml.jackson.databind;


	provides JsonFactory with JsonFactory;
}
