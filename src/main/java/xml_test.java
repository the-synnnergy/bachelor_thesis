import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

import static XMLParser.iTrustParser.get_NameFileMap;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {
        Map<String,String> a = get_NameFileMap(null,"/home/marcel/Downloads/iTrust/source_req.xml");
    }
}
