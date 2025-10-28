package com.cabolabs.openehr.dto_1_0_2.demographic

import com.cabolabs.openehr.rm_1_0_2.demographic.Actor
import com.cabolabs.openehr.rm_1_0_2.demographic.Agent

/**
 * @author pablo.pazos@cabolabs.com
 *
 */
class AgentDto extends ActorDto {

   /**
   * Creates an instance of this class to help the parent Actor toActor() method.
   */
   @Override
   protected Actor createActor()
   {
      return new Agent()
   }  
}
