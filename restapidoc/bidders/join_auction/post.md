# Create/Modify Auction

If Bidder and Auction exist, add the Auction to the Bidder joined auction.

**URL** : `/bidders/$id/join_auction`

**Method** : `POST`

**Auth required** : NO

**Permissions required** : None

**Data example** 

```json
{
    "auctionId": "1"
}
```

## Success Response

**Condition** : Bidder and Auction exist for their respective ids.

**Code** : `201 CREATED`

**Content example**

```json
{
    "description": "Bidder 1 joined auction 1.",
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

**Condition** : No Bidder exists in the system for bidderId.

**Code** : `404 NOT FOUND`

**Content example**

```json
{
    "description": "No action performed. Bidder 2 does not exists.",
    "success": false
}
```

### Or

**Condition** : No Auction exists in the system for auctionId.

**Code** : `404 NOT FOUND`

**Content example**

```json
{
    "description": "Auction 2 does not exist",
    "success": false
}
```

