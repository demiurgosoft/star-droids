package Agents;

import com.eclipsesource.json.*;
import es.upv.dsic.gti_ia.core.ACLMessage;
import helpers.Pair;
import java.util.ArrayList;

/**
 * Clase que abstrae el rol de los agentes.
 * @author Andres Ortiz, Vicente Martínez, Alba Rios, Alberto Meana, Rafael Ruiz
 */
public abstract class Role {
    
    protected Sensors datos;
    protected ActionsEnum action; // Almacena los valores de la heurística
    protected int[][] radar;
    
    protected boolean found; // Meta vista
    protected Pair <Integer, Integer> goal;
    private int[] roles;
    
    //Variables para secondLogic
    protected int[][] miniRadar;

    /**
     * Constructor por defecto del rol.
     * @author Alberto Meana
     */
    public Role() {
        this.datos = new Sensors();
        this.action = ActionsEnum.sleep;
        this.roles = new int[4];
        this.found = false;
        this.goal = new Pair(-1,-1);
        
        for(int i = 0 ; i< 4 ; i++)
        {
            this.roles[i]=-1;
        }
        
        miniRadar = new int[3][3];
    }

    /**
     * Abstraccion de la primera logica en base al rol
     * @author Andres Ortiz
     */
    public abstract void firstLogic();
    
    /**
     * Abstraccion de la segunda logica en base al rol
     * @author Andres Ortiz
     */
    public abstract void secondLogic();
    
    /**
     * Abstraccion de la comprobacion de la meta
     * @author Andres Ortiz
     */
    protected abstract void isFound();
    
    /**
     * Abstraccion del paso de coordenadas de mundo a cartesianas.
     * @author Alba Rios
     */
    protected abstract Pair<Integer,Integer> mapToWorld (int x, int y);
    
    // Get & Set ( Alberto Meana, Alba Rios, Vicente Martinez)
    public ActionsEnum getAction() {
        return this.action;
    }
    
    public Pair<Integer,Integer> getPosition() {
        return this.datos.getPosition();
    }
    
    public Pair<Integer,Integer> getGoalPosition() {
        return this.datos.getGoalPosition();
    }
    
    public Pair<Integer,Integer> [] getShipsPosition() {
        return this.datos.getAllShips();
    }
    
    public Integer[][] getMap() {
        return this.datos.getWorldMap();
    }
    
    public int getMapPosition( int x, int y ) {   
        return this.datos.getMapPosition( x,y );
    }
    
    public boolean getFound() {
        return this.found;
    }
    
    public Pair<Integer,Integer> getShipPosition( int ship ) {
        return this.datos.getShipPosition( ship );
    }
    
    /**
     * @author Andrés Ortiz, Alba Rios
     * @return True si se encuentra en la meta
     */
    public boolean inGoal() {
        Pair<Integer,Integer> myPosition = this.datos.getPosition();
        return (datos.getMapPosition(myPosition.first, myPosition.second) == 3);
    }
    
     /**
     * @author Andrés Ortiz
     * @return True si todos estan en meta
     */
    public boolean allInGoal(){
       Pair<Integer,Integer> [] shipPos=datos.getAllShips();
       
       if(!inGoal()) return false; 
       for(Pair<Integer,Integer> pos : shipPos){
            if(datos.getMapPosition(pos.first, pos.second) != 3) return false;
        }
        return true;
    }
    
    /**
     * @author Andrés Ortiz
     * @return True no queda bateria global o en agentes
     */
    public boolean noFuel(){
           if(datos.getFuel()>0 && datos.getGlobalFuel()>0) return false;
           for(int i:datos.getFuelShips()){
               if(i>0) return false;
           }
           return true;
    }
    
    /**
     * @author Alba Rios
     * @param x Coordenada 
     * @param y Coordenada
     * @return True si hay una nave en x,y
     */
    protected boolean checkShips(int x, int y) {
        Pair<Integer,Integer>[] ships = this.datos.getAllShips();
        
        for(Pair<Integer,Integer> pos : ships) {
            if (pos.first == x && pos.second == y) return true;
        }
        
        return false;
    }
    
    /**
     * @author Rafael Ruiz, Andrés Ortiz
     * @description rellena la estructura de datos de los sensores a partir de un mensaje
     * @param in mensaje que se procesa
     */
    public JsonObject fillSensors(ACLMessage in) {
        JsonObject message = new JsonObject();
        
        message = Json.parse(in.getContent()).asObject().get("result").asObject();
        this.datos.setFuel(message.getInt("battery", 0));
        this.datos.setGlobalFuel(message.getInt("energy", 0));
        this.datos.setGoal(message.getBoolean("goal", false));
        this.datos.setPosition(message.getInt("x", 0), message.getInt("y",0));
        
        JsonArray sensor = Json.parse(in.getContent()).asObject().get("result").asObject().get("sensor").asArray();
        fillDatesRole(sensor);
        
        return message;
    }
  
    /**
     * 
     * @author Andrés Ortiz
     * @description metodo particular de cada rol para rellenar sus datos
     */
    protected abstract void fillDatesRole(JsonArray sensor); //rellena los datos dependiendo del rol
    
    /**
     * @author Alba Rios
     */
    protected abstract void calculateMiniRadar();
         
    /**
     * Conversion del token a la ED.
     * @author Rafael Ruiz, Alberto Meana
     * @param obj el token que se convierte
     */
    public void parseTokenAgent(Token obj) {
        ArrayList<JsonObject> aux = obj.getShipData(); 
                
        for( int ship_number = 0; ship_number < 4; ship_number++ ){
        
            if(aux.get( ship_number ).toString().length() > 2) {
            
                JsonObject sensor = aux.get( ship_number );

                this.datos.setFuelShip( ship_number, sensor.getInt("battery", 0));

                this.datos.setShipPosition( sensor.getInt("x", 0), sensor.getInt("y", 0), ship_number );

                JsonArray pepe = aux.get( ship_number ).get("sensor").asArray();

                if(pepe.size()==9) {
                    fillDatesShips(1, 2, pepe, this.datos.getShipPosition( ship_number ));
                    this.roles[ship_number]=0;
                }
                else if(pepe.size()==25) {
                    fillDatesShips(2, 3, pepe, this.datos.getShipPosition( ship_number ));
                    this.roles[ship_number]=1;
                }
                else if(pepe.size() == 121) {
                    fillDatesShips(5, 6, pepe, this.datos.getShipPosition( ship_number ));
                    this.roles[ship_number]=2;
                }
                else {
                    System.out.println("----------------- 1 Algo malo pasaaaa");
                }
            }

        }
    }
    
    /**
     * Metodo que devuelve los roles de todos los agentes
     * @author Rafael Ruiz
     * @return Los roles de los agentes 
     */
    public int[] getRoles()
    {
        return this.roles;
    }
    
        
    /**
     * Metodo que rellena el mundo conforme a lo escaneado
     * @author Rafael Ruiz, Alberto Meana
     * @param a Elemento que se resta a la posicion actual para los bucles for.
     * @param b Elemento que se suma a la posicion actual para los bucles for.
     * @param sensor Los elementos que se procesan.
     * @param n La posicion actual de la nave
     */
    protected void fillDatesShips(int a, int b, JsonArray sensor, Pair<Integer,Integer> n) {
        int x = n.first;
        int y = n.second;

        int index = 0;
        
        for(int j = y-a ; j < y+b; j++) {
            for(int i = x-a ; i < x+b; i++) {
                if(i>=0 && i<=499 && j>=0 && j<=499){
                
                    int theIndex = sensor.get(index).asInt();
                    if( theIndex == 3 && this.found == false ){
                        this.found = true;
                        this.goal.first = x;
                        this.goal.second = y;
                        this.datos.setGoalPosition( x,y  );
                    }
                    this.datos.setWorldMap(i, j, theIndex);
                }
                    
                index++;
            }
        }
    }
}
