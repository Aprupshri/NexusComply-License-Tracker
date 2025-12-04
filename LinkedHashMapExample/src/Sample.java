import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Sample {
public static void main(String args[]) {
    Scanner sc=new Scanner(System.in);
    LinkedHashMap<Integer,String> map=new LinkedHashMap<>();

    //for(int i=0;i<3;i++)
    while(true)
    {
        String line=sc.nextLine();
        String[] entry=line.split("\\s+",2);
        if(!map.containsKey(Integer.parseInt(entry[0])) && entry.length==2)
            map.put(Integer.parseInt(entry[0]),entry[1]);
        else
        {
            System.out.println("Key already present!");
            break;

        }
    }

    int deleteKey=sc.nextInt();

    if(map.containsKey(deleteKey))
    {
        map.remove(deleteKey);
    }
    else {
        System.out.println("key not found to delete");
    }
    for(int key: map.keySet()){
        System.out.println(key+" "+map.get(key));
    }
}
}
