package gsn.data.discovery


import scala.xml.XML
import scala.xml.Text
import scala.xml.Null
import scala.xml.Elem
import scala.xml.Attribute
import scala.util.Random
import sun.org.mozilla.javascript.internal.xmlimpl.XML

class VsGenerator {
}

object VsGenerator {

  def generate(folderPath: String, count:Integer) = {
    
    val r = Random
    
    for (i <- 1 to count) {
      val fileName = folderPath + "VirtualSensor_" + i
      generateXMLDefFile(fileName + ".xml", i,r.nextInt(10))        
    }
  }
  
  private def generateXMLDefFile(fileName:String, parameter:Integer, sensorType:Int) {
    val vsName = "vs" + parameter
      val vs = 
        <virtual-sensor name={vsName} priority="10">
          <processing-class>
          <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>
          <init-params/>
          <output-structure>
					{
           sensorType match {
              case 0 => 
                <field name="air_tmp" unit="kelvin" type="double"/>
                <field name="soil_tmp" unit="kelvin" type="double"/>
                <field name="snow_tmp" unit="kelvin" type="double"/>
              case 1 =>
                <field name="air_tmp" unit="kelvin" type="double"/>
                <field name="wind_speed" unit="kmph" type="double"/>
              case 2 =>
                <field name="soil_tmp" unit="kelvin" type="double"/>
                <field name="snow_tmp" unit="kelvin" type="double"/>
              case 3 =>
                <field name="rock_tmp" unit="kelvin" type="double"/>
                <field name="air_tmp" unit="kelvin" type="double"/>
              case 4 =>
                <field name="sea_water_volume" unit="m3" type="double"/>
                <field name="sea_ice_volume" unit="m3" type="double"/>
              case 5 =>
                <field name="flow_height" unit="m" type="double"/>
                <field name="snow_thermal_energy_cont" unit="J" type="double"/>
                <field name="sea_floor" unit="m" type="double"/>
              case 6 =>
                <field name="angle_rotation_east_to_x" unit="rad" type="double"/>
                <field name="angle_rotation_east_to_y" unit="rad" type="double"/>
                <field name="angstrom_exp_ambient_aerosol_air" type="double"/>
              case 7 =>
                <field name="area_frac" type="double"/>
                <field name="area_frac_below_surface" type="double"/>
                <field name="a_type" type="double"/>
              case 8 =>
                <field name="atm_abs_opt_thick_due_to_ambient_aerosol" type="double"/>
                <field name="atm_abs_opt_thickness_due_to_black_carbon_ambient_aerosol" type="double"/>
                <field name="atm_cloud_liquid_water_cont" type="double"/>
                <field name="atm_enthalpy_cont" type="double"/>
                <field name="atm_heat_diffusivity" type="double"/>
                <field name="atm_horizontal_velocity_potential" type="double"/>
                <field name="atm_hybrid_height_coord" type="double"/>
                <field name="atm_hybrid_sigma_pressure_coord" type="double"/>
                <field name="atm_kinetic_energy_content" type="double"/>
                <field name="atm_in_pressure_coord" type="double"/>
              case 9 =>
                <field name="atm_mass_content_of_acetic_acid" type="double"/>
                <field name="atm_mass_content_of_aceto_nitrile" type="double"/>
                <field name="atm_mass_content_of_alkanes" type="double"/>
              case _ =>
            }
          }
            
          </output-structure>
          </processing-class>
          <description>
          This sensor simulates light and temperature readings
        every one second.
          </description>
          <life-cycle pool-size="10"/>
          <addressing>
          <predicate key="geographical">Sensor {parameter} @ EPFL</predicate>
          <predicate key="LATITUDE">{randomDouble(-90,90)}</predicate>
          <predicate key="LONGITUDE">{randomDouble(-180,180)}</predicate>
          </addressing>
          <storage history-size="5m"/>
          <streams>
          <stream name="input1">

            {
              sensorType match {
                case 0 => 
                  <source alias="source1" sampling-rate="1" storage-size="1">
           	 				<address wrapper="multiformat"></address>
                  	<query>SELECT light as air_tmp, temperature as soil_tmp, packet_type as snow_tmp, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 1 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as air_tmp, packet_type as wind_speed, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 2 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as soil_tmp, temperature as snow_tmp, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 3 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT temperature as rock_tmp, packet_type as air_tmp, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 4 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as sea_water_volume, temperature as sea_ice_volume, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 5 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as flow_height, temperature as snow_thermal_energy_cont, packet_type as sea_floor, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 6 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as angle_rotation_east_to_x, temperature as angle_rotation_east_to_y, packet_type as angstrom_exp_ambient_aerosol_air, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 7 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as area_frac, temperature as area_frac_below_surface, packet_type as a_type, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 8 =>
                 <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as atm_abs_opt_thick_due_to_ambient_aerosol, temperature as atm_abs_opt_thickness_due_to_black_carbon_ambient_aerosol, packet_type as atm_cloud_liquid_water_cont, light as atm_enthalpy_cont, temperature as atm_heat_diffusivity, packet_type as atm_horizontal_velocity_potential, light as atm_hybrid_height_coord, temperature as atm_hybrid_sigma_pressure_coord, packet_type as atm_kinetic_energy_content, light as atm_in_pressure_coord, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>;
                case 9 =>
                  <source alias="source1" sampling-rate="1" storage-size="1">
                    <address wrapper="multiformat"></address>
                    <query>SELECT light as atm_mass_content_of_acetic_acid, temperature as atm_mass_content_of_aceto_nitrile, packet_type as atm_mass_content_of_alkanes, timed FROM wrapper</query>
                  </source>
                  <query>SELECT * FROM source1</query>
                case _ =>
              }
          }
          </stream>
          </streams>
        </virtual-sensor>
       
        XML.save(fileName, vs, "UTF-8", false, null)
  }
  
  private def randomDouble(start:Integer, end:Integer):Double = {
    val random:Double = new Random().nextDouble()
    start + (random * (end - start))
  }
}