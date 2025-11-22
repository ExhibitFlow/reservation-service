# PostGIS Integration for Venue Mapping - Microservices Architecture

## Overview
This system integrates with a separate **Stall Service** that uses PostGIS for spatial database capabilities. The Reservation service displays an exhibition venue map by fetching stall data with spatial information (location and boundaries) from the Stall service, then combining it with local reservation status.

## Architecture

### Microservices Setup
- **Stall Service** (Port 8082): Manages stall data with PostGIS spatial fields
- **Reservation Service** (Port 8080): Manages reservations and displays venue map
- **Communication**: REST API (Feign Client) + Kafka events

### Data Flow
1. Stall Service stores stalls with PostGIS geometry (Point, Polygon)
2. Reservation Service fetches stall data via REST API
3. Reservation Service determines actual availability from local reservations
4. Stall Service publishes Kafka events when stalls are created/updated
5. Reservation Service invalidates cache on receiving stall events

## What Was Added

### 1. Dependencies (pom.xml)
- **hibernate-spatial**: Hibernate support for spatial data types
- **postgis-jdbc**: PostGIS JDBC driver (for future use if needed)
- **jts-core**: Java Topology Suite for geometry operations

### 2. Updated StallDto
Enhanced `StallDto` to receive spatial data from Stall service:
- `location` (CoordinateDto): Center point with longitude/latitude
- `boundary` (List<List<Double>>): Polygon coordinates in GeoJSON format
- Additional fields: `zone`, `floorNumber`, `description`

### 3. StallServiceClient
Added methods to fetch stall data with spatial information:
- `getAllStalls()`: Get all stalls for venue map
- `getStallsByFloor(Integer)`: Get stalls filtered by floor
- `getStallsByZone(String)`: Get stalls filtered by zone

### 4. Kafka Event DTOs
- **StallCreatedEvent**: Received when new stall is created
- **StallUpdatedEvent**: Received when stall is updated
Both include full spatial data (location, boundary)

### 5. StallEventConsumer
Kafka consumer that handles stall events:
- Listens to `stall.created` and `stall.updated` topics
- Invalidates Feign client cache to ensure fresh data
- Logs all stall changes for monitoring

### 6. VenueMapService
Service that combines stall data with reservation status:
- Fetches stalls from Stall service via Feign client
- Queries local reservations to determine actual availability
- Overrides `isReserved` status based on local data
- Calculates map bounds and statistics
- Provides filtered views (by floor, zone, available only)

### 7. VenueMapController
REST endpoints for venue map:
- `GET /api/venue/map` - Complete venue map
- `GET /api/venue/map/floor/{floorNumber}` - Map by floor
- `GET /api/venue/map/zone/{zone}` - Map by zone
- `GET /api/venue/map/available` - Available stalls only

### 8. DTOs for Map Display
- **StallMapDto**: Stall data with spatial coordinates for map rendering
- **VenueMapResponse**: Complete response with stalls, bounds, and statistics

## Setup Instructions

### 1. Configure Stall Service URL
Update `application.properties`:
```properties
services.stall-service.url=http://localhost:8082
```

### 2. Configure Kafka Topics
Topics for stall events (already configured):
```properties
kafka.topics.stall-created=stall.created
kafka.topics.stall-updated=stall.updated
```

### 3. Start Services
```bash
# Start Kafka
docker-compose up -d

# Start Stall Service (on port 8082)
# The Stall service should have PostGIS configured and publish Kafka events

# Start Reservation Service
./mvnw spring-boot:run
```

## Stall Service Requirements

The Stall service should:

### 1. Use PostGIS Database
- PostgreSQL with PostGIS extension
- Store stalls with `GEOMETRY(Point, 4326)` and `GEOMETRY(Polygon, 4326)`

### 2. Provide REST API Endpoints
```
GET /api/stalls                    - Get all stalls
GET /api/stalls/{id}               - Get stall by ID
GET /api/stalls/floor/{floorNumber} - Get stalls by floor
GET /api/stalls/zone/{zone}        - Get stalls by zone
PUT /api/stalls/{id}/reserve       - Mark stall as reserved
PUT /api/stalls/{id}/release       - Release stall reservation
```

### 3. Publish Kafka Events
When stalls are created or updated, publish events to:
- Topic: `stall.created`
- Topic: `stall.updated`

Event payload should include:
```json
{
  "stallId": 1,
  "stallCode": "A1",
  "size": "Small",
  "price": 500.00,
  "zone": "Zone A",
  "floorNumber": 1,
  "description": "Corner stall",
  "location": {
    "longitude": -0.001000,
    "latitude": 0.000000
  },
  "boundary": [
    [-0.001025, -0.000025],
    [-0.000975, -0.000025],
    [-0.000975, 0.000025],
    [-0.001025, 0.000025],
    [-0.001025, -0.000025]
  ],
  "timestamp": 1700000000000
}
```

## API Usage Examples

### Get Complete Venue Map
```bash
curl http://localhost:8080/api/venue/map
```

Response includes:
- List of all stalls with coordinates and boundaries
- Map bounds (min/max latitude/longitude)
- Statistics (total/available/reserved counts, zones, floors)

### Get Map for Specific Floor
```bash
curl http://localhost:8080/api/venue/map/floor/1
```

### Get Available Stalls Only
```bash
curl http://localhost:8080/api/venue/map/available
```

## Response Format

The venue map API returns stall data with spatial coordinates:

```json
{
  "stalls": [
    {
      "id": 1,
      "stallCode": "A1",
      "size": "Small",
      "price": 500.00,
      "isReserved": false,  // Determined by Reservation service
      "zone": "Zone A",
      "floorNumber": 1,
      "description": "Corner stall with good visibility",
      "location": {
        "longitude": -0.001000,
        "latitude": 0.000000
      },
      "boundary": [
        [-0.001025, -0.000025],
        [-0.000975, -0.000025],
        [-0.000975, 0.000025],
        [-0.001025, 0.000025],
        [-0.001025, -0.000025]
      ]
    }
  ],
  "bounds": {
    "minLongitude": -0.001025,
    "maxLongitude": -0.000575,
    "minLatitude": -0.000150,
    "maxLatitude": 0.000150
  },
  "statistics": {
    "totalStalls": 15,
    "availableStalls": 12,
    "reservedStalls": 3,
    "zones": ["Zone A", "Zone B", "Zone C"],
    "floors": [1, 2]
  }
}
```

**Note**: The `isReserved` field is determined by the Reservation service based on local reservation data, not from the Stall service.

## Frontend Integration

### Displaying the Map
The response format is compatible with mapping libraries like:
- **Leaflet**: Use coordinates directly
- **Google Maps**: Convert to LatLng objects
- **Mapbox**: Use GeoJSON format

### Example with Leaflet
```javascript
fetch('http://localhost:8080/api/venue/map')
  .then(response => response.json())
  .then(data => {
    data.stalls.forEach(stall => {
      // Create polygon from boundary
      const polygon = L.polygon(stall.boundary, {
        color: stall.isReserved ? '#999' : '#3388ff',
        fillOpacity: stall.isReserved ? 0.3 : 0.5
      }).addTo(map);
      
      // Add popup with stall info
      polygon.bindPopup(`
        <b>${stall.stallCode}</b><br>
        Size: ${stall.size}<br>
        Price: $${stall.price}<br>
        Status: ${stall.isReserved ? 'Reserved' : 'Available'}
      `);
    });
    
    // Fit map to bounds
    map.fitBounds([
      [data.bounds.minLatitude, data.bounds.minLongitude],
      [data.bounds.maxLatitude, data.bounds.maxLongitude]
    ]);
  });
```

## Color Coding for Frontend
- **Available stalls**: Green (#4CAF50) or Blue (#3388ff)
- **Reserved stalls**: Gray (#999999) with reduced opacity
- **Hover effect**: Highlight stall boundary
- **Click**: Show stall details and reservation option

## Next Steps
1. **Develop Stall Service**: Implement PostGIS-enabled stall management service
2. **Real-time Updates**: WebSocket for live venue map updates when reservations change
3. **Advanced Queries**: Implement spatial queries (find stalls near entrance, etc.)
4. **Floor Plans**: Add support for venue floor plan overlay images
5. **Admin Interface**: Allow venue managers to create/update stall locations visually
6. **Mobile Support**: Optimize map rendering for mobile devices
7. **3D Visualization**: Consider 3D venue mapping for multi-floor exhibitions
