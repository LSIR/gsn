# GSN Global Sensor Networks : the Core

The Core of GSN, manages the wrappers, virtual-sensors, data-processing workflows and storage. A data item is called a stream-element, it is equivalent to a database row (fixed number of fields with defined types) with a timestamp. 

## Communication

With other Cores it can communicate synchronously (acknowledging every stream element for reliability) or asynchronously (registering to read-only streams) for speed and efficiency. The same communication mechanisms are used to stream data between virtual sensors inside a GSN instance or between instances, thus making the migration of virtual-sensor easier. This communication is meant to stay on a secured network (usually the rack internal network) as GSN doesn't provide encryption or access control at this level. 

It can also query the Services API of another GSN instance, through for example https and using the access control mechanisms. This should be preferred in the case of cross-sites connection or for getting data from a third-party GSN instance.

Using the remote-push wrapper one can also receive data from the Services API, allowing the authenticated users to push stream-elements.

