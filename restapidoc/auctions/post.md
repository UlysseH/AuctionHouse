# Create/Modify Auction

If the id of the item does not already exist, creates the Auction. If it already exist, update the other fields.

**URL** : `/auctions/`

**Method** : `POST`

**Auth required** : NO

**Permissions required** : None

**Data constraints**

Date format must follow pattern : "yyyy-MM-dd HH:mm:ss".

**Data example** All fields must be sent.

```json
{
    "itemId": "auction-1",
    "floorPrice": 100.0,
    "incrementPolicy": 1.0,
    "startDate":"2018-12-31 01:00:58",
    "endDate":"2019-01-23 01:00:58"
}
```

## Success Response

**Condition** : If an actor does not already exist for this itemId.

**Code** : `201 CREATED`

**Content example**

```json
{
    "description": "Auction auction-1 created.",
    "success": true
}
```

### Or

**Condition** : If an Auction already exist for this itemId.

**Code** : `200 OK`

**Content example**

```json
{
    "description": "Updated Auction auction-1.",
    "success": true
}
```

## Error Responses

**Condition** : If fields are missing or type are not correct.

**Code** : `400 BAD REQUEST`
