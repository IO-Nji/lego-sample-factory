Application Error Diagnosis: API Routing, Configuration, and Frontend Issues
The application exhibits several issues primarily centered around API communication, indicative of misconfigurations in the API Gateway's routing rules or the Nginx root proxy. While user authentication and order creation appear to work, many data-loading and management functionalities are failing with 404 Not Found errors for API calls or incorrect frontend routing.

A. Core Problem: Double /api/ Prefix in API Gateway Requests
Symptom: API calls intended for backend services receive a 404 Not Found response, and the reported path in the error shows a duplicated /api/ prefix.

Observed Errors:

Product Loading Failure: "Products do not load in the products page."
Browser Error: "Failed to load products"
Backend Response Examples:
json
Copy
{
    "timestamp": "2025-12-14T22:24:36.923+00:00",
    "path": "/api/api/masterdata/product-variants",
    "status": 404,
    "error": "Not Found",
    "requestId": "efa3ce98-752"
}
{
    "timestamp": "2025-12-14T22:24:36.953+00:00",
    "path": "/api/api/masterdata/modules",
    "status": 404,
    "error": "Not Found",
    "requestId": "883e1bea-753"
}
{
    "timestamp": "2025-12-14T22:24:36.952+00:00",
    "path": "/api/api/masterdata/parts",
    "status": 404,
    "error": "Not Found",
    "requestId": "45c924a9-754"
}
Likely Cause:
Nginx Root Proxy Misconfiguration: The nginx-root-proxy is forwarding requests to the API Gateway with the /api/ prefix already present.
API Gateway Route Misconfiguration: The API Gateway itself is then also adding /api/ to the path when routing to backend services, or its predicates are incorrect.
Result: The actual microservice receives a request like /api/api/masterdata/product-variants, which is an unrecognized path.
Impact: Prevents loading of critical master data (products, modules, parts), inventory data, and other API-driven information.

B. Secondary Problem: Missing Frontend Routes & Authentication Session Management
Symptom: Some frontend pages return 404 Not Found and user management functions fail despite successful authentication.

Observed Errors:

"Broken Link" (Frontend 404):
Request: GET http://192.168.1.237/login
Response: Status Code: 404 Not Found from nginx/1.29.4.
Users Management Failure:
"Manage Existing Users says 'No user found'." (Implies API call to fetch users failed or returned empty)
"Users Status show no statistics." (Implies API call to fetch statistics failed)
"Create new user error: 'Unable to create user. Confirm your admin session is valid.'" (Authentication issue, or API call failed before reaching user service logic).
Request Details: Includes a valid Bearer token in the Authorization header, but the issue is likely a 404 Not Found or 401 Unauthorized deeper in the call chain.
Likely Cause:
Frontend Routing Misconfiguration in Nginx: The nginx-root-proxy is failing to correctly route non-API paths (like /login) to the frontend application's client-side router (React Router). The try_files directive is either missing or incorrectly applied in the nginx-root-proxy, preventing the SPA from handling its own routes.
API Gateway Routing for User Service: Even if the authentication (login) worked, API calls for managing users might also be subject to the same double /api/ issue or an incorrect route definition in the API Gateway.
Session/Token Handling: While a token is present, the "admin session invalid" message could be a generic error from the API Gateway if it fails to validate the token against the user-service due to routing issues, or if user-service itself is not reachable.
Impact: Core user management is non-functional, hindering administrative tasks and potentially showing a broken application state.

C. Specific Data Loading Failures
Symptom: Dashboards and inventory tables do not update or show data.

Observed Errors:

Warehouse Dashboard:
"Plant Warehouse user dashboard: Current Inventory Table is not updated after orders are created."
"Warehouse Management says: 'Failed to load warehouses. Please try again.'"
Factory Admin Dashboard:
"Total Orders, Pending Orders, Completed Orders, Recent Production Orders: do not update."
Likely Cause:
Underlying API Call Failures: These symptoms are almost certainly secondary effects of failed API calls (most likely 404 Not Found due to the double /api/ prefix) to the inventory-service and order-processing-service. If the frontend cannot fetch the data, it cannot display it.
Impact: Reduces observability and operational control, making the application appear incomplete or broken.

Summary of Underlying Issues:
Nginx Root Proxy Path Handling: The nginx-root-proxy's location / block is not correctly set up for SPA (React) routing, leading to 404 for frontend routes like /login. It also appears to be either incorrectly stripping/adding /api/ before passing to the API Gateway.
API Gateway Routing/Predicates: The API Gateway is likely receiving paths already prefixed with /api/ from the Nginx root proxy, and then incorrectly adding another /api/ (or its predicates are too broad/specific).
Inter-Service Communication (within Docker): While User Authentication works (meaning user-service is reachable by api-gateway for login), other calls might fail due to the path issues.
The primary focus for fixing should be on the nginx-root-proxy/nginx.conf and the api-gateway's routing configuration.