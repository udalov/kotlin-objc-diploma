#import <Foundation/NSObject.h>

typedef BOOL (*fun_t)();

@interface A : NSObject
+ (void) printOIfTrue: (fun_t) fun;
+ (void) printKIfFalse: (fun_t) fun;
@end
