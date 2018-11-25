import com.fasterxml.jackson.core.JsonFactory;

module com.fasterxml.jackson.core {
	exports com.fasterxml.jackson.core;

	exports com.fasterxml.jackson.core.type to com.fasterxml.jackson.databind, com.jwebmp.core, com.fasterxml.jackson.datatype.jsr310;
	exports com.fasterxml.jackson.core.io to com.fasterxml.jackson.databind, com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jdk8;
	exports com.fasterxml.jackson.core.util to com.fasterxml.jackson.databind, com.fasterxml.jackson.module.paranamer, com.fasterxml.jackson.module.parameternames, com.fasterxml.jackson.module.osgi, com.fasterxml.jackson.module.mrbean, com.fasterxml.jackson.module.jaxb, com.fasterxml.jackson.module.afterburner, com.fasterxml.jackson.datatype.jsr310, com.fasterxml.jackson.datatype.jdk8, com.fasterxml.jackson.module.guice;
	exports com.fasterxml.jackson.core.base  to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.json  to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.filter to com.fasterxml.jackson.databind;
	exports com.fasterxml.jackson.core.format  to com.fasterxml.jackson.databind;


	provides JsonFactory with JsonFactory;
}
