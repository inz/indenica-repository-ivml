/**
 * 
 */
package eu.indenica.runtime.api;

import javax.jws.WebService;


/**
 * @author Christian Inzinger
 *
 */
@WebService
public interface IIvmlApi {
    void storeModel(String model);
    String getModel(String name);
}
