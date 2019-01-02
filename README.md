# Auction House system REST API documentation

The service is running on 'http://localhost:5000/'.

## Open Endpoints

All the endpoints are accesible without any login.

### Bidder related

### Auction related

Endpoints for viewing and manipulating the Accounts that the Authenticated User
has permissions to access.

* [Show Existing Auctions](restapidoc/accounts/get.md) : `GET /api/accounts/`
* [Create/Update Auction](restapidoc/accounts/post.md) : `POST /api/accounts/`
* [Show An Account](accounts/pk/get.md) : `GET /api/accounts/:pk/`
* [Update An Account](accounts/pk/put.md) : `PUT /api/accounts/:pk/`
* [Delete An Account](accounts/pk/delete.md) : `DELETE /api/accounts/:pk/`
