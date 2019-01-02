# Create/Modify Auction

If the id of the item does not already exist, creates the Auction. If it already exist, update the other fields.

**URL** : `/auctions/`

**Method** : `POST`

**Auth required** : NO

**Permissions required** : None

**Data example** 

```json
{
	"bidderId": "2"
}
```

## Success Response

**Condition** : If there is no Bidder in the system for this bidderId.

**Code** : `201 CREATED`

**Content example**

```json
{
    "description": "Bidder 2 created.",
    "success": true
}
```

## Error Responses

**Condition** : If the field is missing or of incorrect type.

**Code** : `400 BAD REQUEST`

**Content example**

```
The request content was malformed:
Expected String as JsString, but got 12
```

### Or

**Condition** : If there is already a Bidder in the system for this bidderId.

**Code** : `409 Conflict`

**Content example**

```json
{
    "description": "Bidder already exists !",
    "success": false
}
```
