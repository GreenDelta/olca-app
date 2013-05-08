package org.openlca.core.application.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.transform.JDOMSource;
import org.openlca.core.database.IDatabaseServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A file for storing the database connections. */
class ServerFile {

	private Logger log = LoggerFactory.getLogger(getClass());
	private File file;

	public ServerFile(File file) {
		this.file = file;
	}

	public <T extends IDatabaseServer> List<T> read(Class<T> clazz) {
		log.trace("read providers from {}", file);
		if (file == null || !file.exists())
			return Collections.emptyList();
		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			List<?> childs = root.getChildren("provider");
			return parseElements(clazz, childs);
		} catch (Exception e) {
			log.error("Failed to read data provider file", e);
			return Collections.emptyList();
		}
	}

	private <T extends IDatabaseServer> List<T> parseElements(Class<T> clazz,
			List<?> childs) throws Exception {
		List<T> providers = new ArrayList<>();
		for (Object child : childs) {
			if (child instanceof Element) {
				Element element = (Element) child;
				T provider = clazz.newInstance();
				mapValues(element, provider);
				providers.add(provider);
			}
		}
		return providers;
	}

	private void mapValues(Element element, IDatabaseServer provider) {
		String name = element.getChildText("name");
		log.trace("read provider {}", name);
		Element propElem = element.getChild("properties");
		if (propElem == null)
			return;
		Map<String, String> props = readProperties(propElem);
		provider.setProperties(props);
	}

	private Map<String, String> readProperties(Element elem) {
		HashMap<String, String> vals = new HashMap<>();
		for (Object child : elem.getChildren("property")) {
			if (child instanceof Element) {
				Element prop = (Element) child;
				String key = prop.getChildText("name");
				String val = prop.getChildText("value");
				vals.put(key, val);
			}
		}
		return vals;
	}

	public <T extends IDatabaseServer> void write(List<T> providers) {
		log.trace("write providers to {}", file);
		try {
			if (!file.exists())
				file.createNewFile();
			Element root = new Element("providers");
			for (IDatabaseServer dataProv : providers) {
				addProvider(root, dataProv);
			}
			writeFile(root);
		} catch (Exception e) {
			log.error("Failed to write provider file", e);
		}
	}

	private void addProvider(Element root, IDatabaseServer dataProv) {
		Element provider = new Element("provider");
		root.addContent(provider);
		Element nameElem = new Element("name");
		nameElem.setText(dataProv.getName(false));
		provider.addContent(nameElem);
		Element properties = new Element("properties");
		provider.addContent(properties);
		addProperties(properties, dataProv);
	}

	private void addProperties(Element propertyElem, IDatabaseServer dataProv) {
		for (Entry<String, String> property : dataProv.getProperties()
				.entrySet()) {
			Element propertyElement = new Element("property");
			Element name = new Element("name");
			name.setText(property.getKey());
			Element value = new Element("value");
			value.setText(property.getValue());
			propertyElement.addContent(name);
			propertyElement.addContent(value);
			propertyElem.addContent(propertyElement);
		}
	}

	private void writeFile(Element root)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException {
		Document document = new Document(root);
		Source source = new JDOMSource(document);
		Result result = new StreamResult(file);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(source, result);
	}

}
