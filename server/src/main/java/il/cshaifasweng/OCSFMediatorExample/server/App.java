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
        DbConnector db = new DbConnector();
        db.AddTestData();

        server = new SimpleServer(3000, db);
        server.listen();
    }
}
