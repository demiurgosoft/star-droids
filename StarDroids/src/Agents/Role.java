package Agents;

import com.eclipsesource.json.*;
import es.upv.dsic.gti_ia.core.ACLMessage;
import helpers.Pair;
/**
 * @author Andres Ortiz, Vicente Martínez
 */
public abstract class Role {
    //Data (if any)

    protected Sensors datos;
    protected AgentAction action; //almacena los valores de la heurística
    protected ActionsEnum direction = ActionsEnum.moveS; //Aux para basic logic

    //Constructor
    public Role() {
        this.datos = new Sensors();
        this.action=new AgentAction();
    }

    //basic logic classes, implement here if common
    public abstract void firstLogic();
    public abstract void secondLogic();
    
    /**
    * @author Andres Ortiz, Alba Rios
    * @description ejemplo de logica básica
    */
    protected void basicLogic(){
        Pair<Integer,Integer> myPos = datos.getPosition();
        int x = myPos.first; int y = myPos.second;
        
        if(datos.inGoal()) action.multiplyAction(ActionsEnum.sleep, 10); //Si esta en el objetivo, esperar

        if(datos.getFuel() == 1) action.multiplyAction(ActionsEnum.battery, 2); //recargar si esta sin bateria

        //Moverse abajo-derecha o arriba-derecha
        if (direction == ActionsEnum.moveS){
            if (datos.getMapPosition(x, y-1) != 1) action.multiplyAction(ActionsEnum.moveS, 2);
            else{
                action.multiplyAction(ActionsEnum.moveE, 2);
                direction = ActionsEnum.moveN;
            }
        }
        if (direction == ActionsEnum.moveN){
            if (datos.getMapPosition(x, y+1) != 1) action.multiplyAction(ActionsEnum.moveN, 2);
            else{
                action.multiplyAction(ActionsEnum.moveE, 2);
                direction = ActionsEnum.moveS;
            }
        }
        
        checkShips();
        updateObstacles();
    }
    
    /**
     * @author Andres Ortiz
     * @description Comprueba naves adyacentes
     */
    protected void checkShips(){
        Pair<Integer,Integer> myPos=datos.getPosition();
        Pair<Integer,Integer>[] ships=datos.getAllShips();
        int x=myPos.first,y=myPos.second; //Esto puede ir fuera?
        for(Pair<Integer,Integer> pos : ships){
            int x2=pos.first,y2=pos.second;

            if(x==x2-1 && y==y2-1) action.setToZero(ActionsEnum.moveNW);
            if(x==x2-1 && y==y2) action.setToZero(ActionsEnum.moveW);
            if(x==x2-1 && y==y2+1) action.setToZero(ActionsEnum.moveSW);
            if(x==x2 && y==y2-1) action.setToZero(ActionsEnum.moveN);
            if(x==x2 && y==y2+1) action.setToZero(ActionsEnum.moveS);
            if(x==x2+1 && y==y2-1) action.setToZero(ActionsEnum.moveNE);
            if(x==x2+1 && y==y2) action.setToZero(ActionsEnum.moveE);
            if(x==x2+1 && y==y2+1) action.setToZero(ActionsEnum.moveSE);
        }
    }
    
    /**
     * @author Alba Rios
     * @description Evita el movimiento hacia obstaculos
     */
    protected void updateObstacles(){
        Pair<Integer,Integer> myPos = datos.getPosition();
        int x = myPos.first; int y = myPos.second;
        
        //Obstaculo == 1
        if (datos.getMapPosition(x-1, y-1) == 1) action.setToZero(ActionsEnum.moveNW);
        if (datos.getMapPosition(x, y-1) == 1) action.setToZero(ActionsEnum.moveN);
        if (datos.getMapPosition(x+1, y-1) == 1) action.setToZero(ActionsEnum.moveNE);
        if (datos.getMapPosition(x-1, y) == 1) action.setToZero(ActionsEnum.moveW);
        if (datos.getMapPosition(x+1, y) == 1) action.setToZero(ActionsEnum.moveE);
        if (datos.getMapPosition(x-1, y+1) == 1) action.setToZero(ActionsEnum.moveSW);
        if (datos.getMapPosition(x, y+1) == 1) action.setToZero(ActionsEnum.moveS);
        if (datos.getMapPosition(x+1, y+1) == 1) action.setToZero(ActionsEnum.moveSE);
    }
    
    /**
     * @author Andres Ortiz
     * @description devuelve la accion a realizar
     */ 
    public ActionsEnum getAction(){
        return action.getAction();
    }
    
    
    /**
     * @author Rafael Ruiz
     * 
     * @param in 
     */
    public void fillSensors(ACLMessage in, Role role)
    {
        //Datos {"result":{"battery":100,"x":83,"y":99,"sensor":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,2,2,2,2],"energy":1000,"goal":false}} 
        JsonObject message = new JsonObject();
        
        message = Json.parse(in.getContent()).asObject().get("result").asObject();
        
        JsonArray sensor = Json.parse(in.getContent()).asObject().get("result").asObject().get("sensor").asArray();
        
        this.datos.setFuel(message.getInt("battery", 0));
        this.datos.setGlobalFuel(message.getInt("energy", 0));
        this.datos.setGoal(message.getBoolean("goal", false));
        this.datos.setPosition(message.getInt("x", 0), message.getInt("y",0));
        //this.datos.setShipPosition(0, 0, 0); // AUN NO SE QUE ES ESTO
        
        
        //relleno mapa de datos segun lo que es
        fillDatesRole(sensor);
       /* if(role.getClass().equals(XWing.class))
        {
            fillDates(1, 2, sensor);
            
        }else if (role.getClass().equals(YWing.class))
        {
            fillDates(2, 3, sensor);
            
        }else if (role.getClass().equals(MillenniumFalcon.class))
        {
            fillDates(5, 6, sensor);
        }*/
        this.datos.Show();
    }
  
    /**
     * 
     * @author Andrés Ortiz
     */
    protected abstract void fillDatesRole(JsonArray sensor); //rellena los datos dependiendo del rol
    
    /**
     * 
     * @author Rafael Ruiz
     * 
     * @param a
     * @param b
     * @param sensor 
     */
    protected void fillDates(int a, int b, JsonArray sensor)
    {
        int x = (Integer) this.datos.getPosition().first;
            
        if(x-a < 0)
        {
            x = x + a;

        }else if(x+b > 500)
        {
            x = 500 - b;
        }


        int y = (Integer) this.datos.getPosition().second;

        if(y-a < 0)
        {
            y = y + a;

        }else if(y+b > 500)
        {
            y = 500 - b;
        }

        int index = 0;


        for(int i = x-a ; i < x+b; i++)
        {
            for(int j = y-a ; j < y+b; j++)
            {
                this.datos.setWorldMap(i, j, sensor.get(index).asInt());

                index++;

            }
        }  
    }
    
    public Pair<Integer,Integer> getPosition(){
        return this.datos.getPosition();
    }
    
    public Pair<Integer,Integer> getGoalPosition(){
        return this.datos.getGoalPosition();
    }
    
    public Pair<Integer,Integer>[][] getMap(){
        return this.datos.getWorldMap();
    }

}
