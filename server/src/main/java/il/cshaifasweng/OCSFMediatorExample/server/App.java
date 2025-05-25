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

        server = new SimpleServer(3000, dbConnector);
        server.listen();
    }
}
