package XMLParser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class iTrustParser
{
    static String path = null;

    public static Map<String,String> get_NameFileMap(String absolute_folder_path, String xml_file) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File xml =  new File(xml_file);
        System.out.println(xml);
        Document doc = builder.parse(xml);
        Element root = doc.getDocumentElement();
        NodeList node_list = root.getElementsByTagName("artifact");
        Element current;
        Map<String,String> name_to_filepath  = new HashMap<>();
        for (int i = 0; i< node_list.getLength();i++){
            current = (Element) node_list.item(i);
            String name = current.getElementsByTagName("id").item(0).getTextContent();
            String path = absolute_folder_path +current.getElementsByTagName("content").item(0).getTextContent();
            name_to_filepath.put(name,path);
        }
        assert name_to_filepath.size() == node_list.getLength();
        return name_to_filepath;
    }

}
