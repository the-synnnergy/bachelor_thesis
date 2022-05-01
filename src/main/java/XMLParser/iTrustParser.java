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
import java.util.*;

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

    public static Map<String, List<String>> get_true_req_to_source_links(String absolute_folder_path, String xml_file) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File xml =  new File(xml_file);
        System.out.println(xml);
        Document doc = builder.parse(xml);
        Element root = doc.getDocumentElement();
        NodeList node_list = root.getElementsByTagName("link");
        Map<String, List<String>> req_to_source = new HashMap<>();
        for(int i = 0;i < node_list.getLength();i++){
            Element current = (Element) node_list.item(i);
            String source_artifact = current.getElementsByTagName("source_artifact_id").item(0).getTextContent();
            String target_artifact = current.getElementsByTagName("target_artifact_id").item(0).getTextContent();
            List<String> tmp = req_to_source.getOrDefault(source_artifact,null);
            if(tmp == null)
            {
                req_to_source.put(source_artifact,new ArrayList<>(Arrays.asList(target_artifact)));
            }else
            {
                tmp.add(target_artifact);
            }
        }
        return req_to_source;
    }

}
