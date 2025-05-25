package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App 
{
    private static DbConnector dbConnector;
	private static SimpleServer server;
    public static void main( String[] args ) throws IOException
    {
        dbConnector = new DbConnector();
        dbConnector.AddTestData();
        Item testItem = new Item();
        testItem.setName("TestItem");
        testItem.setDescription("TestItemDescription");
        testItem.setPrice(-15.24);
        dbConnector.AddItem(testItem);
        List<Item> catalog = dbConnector.GetItemList(new ArrayList<>());
        testItem.setPrice(999.99);
        dbConnector.EditItem(testItem);
        catalog = dbConnector.GetItemList(new ArrayList<>());
        server = new SimpleServer(3000, dbConnector);
        server.listen();
    }
}
