import XMLParser.iTrustCodeParser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;

import static XMLParser.iTrustCodeParser.get_NameFileMap;

public class xml_test
{
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException
    {
        Map<String,String> a = get_NameFileMap(null,"./target_code.xml");
    }
}
